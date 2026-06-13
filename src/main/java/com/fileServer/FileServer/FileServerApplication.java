package com.fileServer.FileServer;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import io.javalin.http.Context;

/**
 * FileServerApplication is the entry point for the local file server.
 *
 * <p>It starts an HTTP server on port 8080 using Javalin and exposes REST APIs
 * for listing, downloading, uploading, and deleting files on the host machine.
 * A web-based terminal UI is served as a static resource, accessible from any
 * browser on the same network.</p>
 *
 * <p>The server exposes the following endpoints:</p>
 * <ul>
 *   <li>GET  /api/files    — list files and folders at a given path</li>
 *   <li>GET  /api/download — download a file</li>
 *   <li>POST /api/upload   — upload one or more files</li>
 *   <li>DELETE /api/delete — delete a file</li>
 * </ul>
 * 
 * @author harsimarpreetsingh
 * @phone +91 7988016430
 * @email harsimarpreet.singh05@gmail.com
 */
public class FileServerApplication 
{

    /**
     * Root directory from which all file operations are performed.
     * Set to "/" to allow navigation of the entire filesystem.
     */
    private static final String ROOT_DIR = "/";

    /**
     * Application entry point. Configures and starts the Javalin HTTP server,
     * registers all API routes, and prints the local IP addresses to the console.
     *
     * @param args command-line arguments (not used)
     * @throws Exception if the server fails to start or network info cannot be retrieved
     */
    public static void main(String[] args) throws Exception 
    {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(8080);

        app.get("/api/files", FileServerApplication::listFiles);
        app.get("/api/download", FileServerApplication::downloadFile);
        app.post("/api/upload", FileServerApplication::uploadFile);
        app.delete("/api/delete", FileServerApplication::deleteFile);

        printLocalIPs();
    }

    /**
     * Lists files and directories at the path specified by the {@code path} query parameter.
     *
     * <p>Returns a JSON array where each entry contains:</p>
     * <ul>
     *   <li>{@code name}  — file or folder name</li>
     *   <li>{@code isDir} — true if it is a directory</li>
     *   <li>{@code size}  — size in bytes (0 for directories)</li>
     *   <li>{@code path}  — relative path from ROOT_DIR</li>
     * </ul>
     * Results are sorted with directories first, then alphabetically.
     *
     * @param ctx the Javalin request context; expects optional query param {@code path}
     */
    private static void listFiles(Context ctx)
    {
        System.out.println("============Listing files========= from IP: " + ctx.ip());
        
        String relativePath = ctx.queryParam("path");
        
        if (relativePath == null) relativePath = "";

        File dir = resolveSafe(relativePath, null);
        
        if (dir == null || !dir.isDirectory())
        {
            ctx.status(400).result("Invalid path");
            
            return;
        }

        File[] files = dir.listFiles();
        
        if (files == null) files = new File[0];

        List<Map<String, Object>> result = new ArrayList<>();
        
        for (File f : files) 
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            
            entry.put("name", f.getName());
            
            entry.put("isDir", f.isDirectory());
            
            entry.put("size", f.isFile() ? f.length() : 0);
            
            entry.put("path", relativePath.isEmpty() ? f.getName() : relativePath + "/" + f.getName());
            
            result.add(entry);
        }

        result.sort((a, b) -> 
        {
            boolean aDir = (boolean) a.get("isDir");
            
            boolean bDir = (boolean) b.get("isDir");
            
            if (aDir != bDir) return aDir ? -1 : 1;
            
            return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
        });

        ctx.json(result);
    }

    /**
     * Streams a file to the client as a binary download.
     *
     * <p>The file is identified by the {@code path} query parameter, resolved
     * relative to ROOT_DIR. Sets appropriate headers for browser download.</p>
     *
     * @param ctx the Javalin request context; expects query param {@code path}
     * @throws IOException if the file cannot be read
     */
    private static void downloadFile(Context ctx) throws IOException 
    {
    	System.out.println("============Downloading files=========");
    	
        String relativePath = ctx.queryParam("path");
        
        File file = resolveSafe(relativePath, null);
        
        if (file == null || !file.isFile()) 
        {
            ctx.status(404).result("File not found");
            
            return;
        }
        
        ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        
        ctx.header("Content-Length", String.valueOf(file.length()));
        
        ctx.contentType("application/octet-stream");
        
        ctx.result(new FileInputStream(file));
    }

    /**
     * Accepts one or more uploaded files and saves them to the directory
     * specified by the {@code path} query parameter.
     *
     * <p>Files are expected as multipart form data under the key {@code file}.
     * Each uploaded file is saved using its original filename.</p>
     *
     * @param ctx the Javalin request context; expects query param {@code path} and multipart file(s)
     * @throws IOException if a file cannot be written to disk
     */
    private static void uploadFile(Context ctx) throws IOException 
    {
    	System.out.println("============Uploading files=========");
    	
        String relativePath = ctx.queryParam("path");
        
        if (relativePath == null) relativePath = "";

        File dir = resolveSafe(relativePath, "Users/harsimarpreetsingh/Desktop");

        if (dir == null || !dir.isDirectory())
        {
            ctx.status(400).result("Invalid target directory");
        
            return;
        }

        if (!dir.canWrite())
        {
            ctx.status(403).result("Cannot upload here — directory is read-only. Navigate to a writable folder first.");
            
            return;
        }

        var uploaded = ctx.uploadedFiles("file");
        
        if (uploaded.isEmpty()) 
        {
            ctx.status(400).result("No file provided");
        
            return;
        }

        for (var upload : uploaded)
        {
            File dest = new File(dir, upload.filename());
            
            try (InputStream in = upload.content(); OutputStream out = new FileOutputStream(dest)) 
            {
                in.transferTo(out);
            }
        }

        ctx.result("Uploaded successfully");
    }

    /**
     * Deletes a file at the path specified by the {@code path} query parameter.
     *
     * <p>Only files can be deleted; directory deletion is not permitted.</p>
     *
     * @param ctx the Javalin request context; expects query param {@code path}
     */
    private static void deleteFile(Context ctx) 
    {
        String relativePath = ctx.queryParam("path");
        
        File file = resolveSafe(relativePath, null);
        
        if (file == null || !file.exists()) 
        {
            ctx.status(404).result("Not found");
            
            return;
        }
        
        if (file.isDirectory()) 
        {
            ctx.status(400).result("Cannot delete directories");
            
            return;
        }
        
        file.delete();
        
        ctx.result("Deleted");
    }

    /**
     * Resolves a relative path against ROOT_DIR and validates it does not
     * escape the root via path traversal (e.g. {@code ../../etc/passwd}).
     *
     * @param relativePath the path to resolve, relative to ROOT_DIR
     * @return the resolved {@link File}, or {@code null} if the path is invalid or outside ROOT_DIR
     */
    private static File resolveSafe
    (
		String relativePath, 
		String appendedIfNeed
	) 
    {
    	File target = null;
    	
        try 
        {
        	String fullPath = ROOT_DIR;
        	
        	if(appendedIfNeed != null)
        	{
        		fullPath = ROOT_DIR + appendedIfNeed;
        		
        		target = new File(fullPath).getCanonicalFile();
        	}
        	else 
        	{
        		File root = new File(fullPath).getCanonicalFile();
            	
                target = relativePath == null || relativePath.isEmpty()
                        ? root
                        : new File(root, relativePath).getCanonicalFile();
        		
        	}
        } 
        catch (IOException e) 
        {
			System.out.print("Exception while resolving the path in resolveSafe, Exception: " + e);
        }
        
        return target;
    }

    /**
     * Prints all active non-loopback IPv4 addresses to the console so the user
     * knows which URLs to open on other devices.
     *
     * @throws Exception if network interface information cannot be retrieved
     */
    private static void printLocalIPs() throws Exception 
    {
        System.out.println("\n=== File Server Running ===");
        
        System.out.println("Serving: " + ROOT_DIR);
        
        System.out.println("Open in browser:");
        
        System.out.println("  Local  -> http://localhost:8080");
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        
        while (interfaces.hasMoreElements()) 
        {
            NetworkInterface ni = interfaces.nextElement();
           
            if (!ni.isUp() || ni.isLoopback()) continue;
            
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            
            while (addresses.hasMoreElements())
            {
                InetAddress addr = addresses.nextElement();
               
                if (addr.getHostAddress().contains(":")) continue;
                
                System.out.println("  Network-> http://" + addr.getHostAddress() + ":8080");
            }
        }
        
        System.out.println("===========================\n");
    }
}
