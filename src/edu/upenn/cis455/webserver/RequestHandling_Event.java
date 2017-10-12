package edu.upenn.cis455.webserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;


public class RequestHandling_Event {

	SocketChannel s;
	String method;
	String req;
	String filename;
	String httpVersion;
	String rootDir;
	String controlPanelList;
	String dirList;
	int port;
	Charset charset;
	SimpleDateFormat[] sdf = new SimpleDateFormat[3];

	public RequestHandling_Event(int port, String rootDir, SocketChannel socketChannel, String request, Charset charset) {
		// TODO Auto-generated constructor stub
		this.s=socketChannel;
		this.req=request;
		StringTokenizer str = new StringTokenizer(request," ");
		this.method=str.nextToken();
		this.filename=str.nextToken();
		String temp=str.nextToken();
		this.httpVersion=temp.substring(0,temp.indexOf("\r\n"));
		this.rootDir=rootDir;
		controlPanelList=null;
		dirList=null;
		this.port=port;
		this.charset=charset;
		sdf[0] = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss z");
		sdf[1] = new SimpleDateFormat("EEEE, d-MMM-yy hh:mm:ss z");
		sdf[2] = new SimpleDateFormat("EEE MMM dd hh:mm:ss yy");
		sdf[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		sdf[1].setTimeZone(TimeZone.getTimeZone("GMT"));
		sdf[2].setTimeZone(TimeZone.getTimeZone("GMT"));

	}

	public boolean processRequest() throws IOException{
		// TODO Auto-generated method stub
		try{
		if(parseRequest())
		{
				//System.out.println("if2");
				if(filename.equals(null) || filename.equals("/favicon.ico")) //check for NULL or favicon requests
					return false;				
				if(filename.substring(0).equals("/control"))// handle control panel request
					{
					displayControlPanel();
					return false;
					}
				else if(filename.substring(0).equals("/shutdown") || filename.substring(0).equals("/shutdown?"))// handle shutdown request
					{
					shutDown();
					return true;
					}
				else
					{
					getFile();
					return false;
					}
			}
		else 
			return false;
		}catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			catch(IOException e)
			{
					send500error();
			}
		return false; 
		}


	private void displayControlPanel() throws IOException {
		// TODO Auto-generated method stub
		String displayString="<html><h2>Currently executing request: "+filename+"</h2><body><form action=\"shutdown\" method=\"get\"><button type=\"submit\">SHUTDOWN</button></form></body></html>";
		s.write(ByteBuffer.wrap((httpVersion+" 200 OK\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Content-Type: text/html\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Content-Length: "+displayString.length()+"\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Date: "+sdf[0].format(new Date())+"\r\n"+displayString.length()+"\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Connection: close\r\n\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap(displayString.getBytes(charset)));
	}

	private void shutDown() throws IOException {
		// TODO Auto-generated method stub
		System.out.println("Shutdown called!");
		String shutdown="<html>Event Server Shutdown!</html>";
		s.write(ByteBuffer.wrap((httpVersion+" 200 OK\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Content-Type: text/html\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Content-Length: "+shutdown.length()+"\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Date: "+sdf[0].format(new Date())+"\r\n"+shutdown.length()+"\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap((httpVersion+"Connection: close\r\n\r\n").getBytes(charset)));
		s.write(ByteBuffer.wrap(shutdown.getBytes(charset)));
	}
		
	private void getFile() throws IOException {// Send the requested file to client if the file
		// exists in the specified path and authorized
		// directory
		// TODO Auto-generated method stub
		// System.out.println("In getfile()");
		boolean isAbsolute = false;
		String reqFile = null;
			if (filename.contains("http://"))
				if (filename.contains("localhost:" + port)) {
					isAbsolute = true;
					int index = filename.indexOf(Integer.toString(port))
							+ Integer.toString(port).length();
					// System.out.println(index);
					reqFile = filename.substring(index);
					// System.out.println("\nRequested file1 : "+reqFile);
				}
			if (!isAbsolute) {
				// System.out.println("Relative path");
				char c = rootDir.charAt(rootDir.length() - 1);
				if (c == '/')
					reqFile = rootDir + filename;
				else
					reqFile = rootDir + "/" + filename;
			}
			String parsedURL = parseURL(reqFile);
			if (parsedURL.contains(rootDir)) {
				// proceed to send file
				File f = new File(parsedURL);
				if (f.isDirectory())
					listFilenames(parsedURL);
				else if (f.exists()) {
					String contentType = null;
					String type = null;
					type = filename.substring(filename.lastIndexOf("."));
					// System.out.println("\nType : "+type);
					// get Type of file
					if (type.equals(".html") || type.equals(".htm"))
						contentType = "text/html";
					else if (type.equals(".jpg") || type.equals(".jpeg"))
						contentType = "image/jpeg";
					else if (type.equals(".gif"))
						contentType = "image/gif";
					else if (type.equals(".png"))
						contentType = "image/png";
					else if (type.equals(".txt"))
						contentType = "text/plain";
					else
						type = null;
					if (type == null) {
						send404error();
						return;
					}
					// System.out.println("Sending file");
					InputStream is = new FileInputStream(f);
					s.write(ByteBuffer.wrap((httpVersion + " 200 OK\r\n").getBytes(charset)));
					s.write(ByteBuffer.wrap((httpVersion + " 200 OK\r\n").getBytes(charset)));
					s.write(ByteBuffer.wrap(("Content-Length: " + f.length() + "\r\n").getBytes(charset)));
						s.write(ByteBuffer.wrap(
								("Content-Type: " + contentType + "\r\n")
								.getBytes(charset)));
						s.write(ByteBuffer.wrap(
								("Date: " + sdf[0].format(new Date()) + "\r\n")
								.getBytes(charset)));
						s.write(ByteBuffer.wrap(
								("Connection: close\r\n\r\n").getBytes(charset)));
					if (method.equals("GET")) {
						// System.out.println("Method check!");
						byte[] buffer = new byte[1024];
						while (is.read(buffer) > 0)
							s.write(ByteBuffer.wrap(buffer));
						is.close();
					}
				} else {
					send404error();
					return;
				}
			} else
				send403error();
	}

	private String parseURL(String url) {// parse url if it is relative. like
		// ../../../
		// TODO Auto-generated method stub
		String[] path = url.split("/");
		int i = 0, j = 0;
		for (i = 0; i < path.length; i++)
			if (path[i].equals("..")) {
				path[i] = null;
				j = i - 1;
				while (j > 0) {
					if (path[j] != null) {
						path[j] = null;
						break;
					} else
						j--;
				}
			}
		String finalPath = null;
		j = path.length;
		for (i = 0; i < j; i++)
			if (path[i] != null)
				if (i == 0)
					finalPath = path[i] + "/";
				else if (i == j - 1)
					finalPath += path[i];
				else
					finalPath += path[i] + "/";
		return finalPath;
	}

	private void send403error() throws IOException {
		// TODO Auto-generated method stub
		String error403 = "<html>403 Error : Forbidden request. Not authorized to view the requested resource.</html>";
			s.write(ByteBuffer.wrap(
					(httpVersion + " 403 Forbidden\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Type: text/html\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Length: " + error403.length() + "\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Date: " + sdf[0].format(new Date()) + "\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(("Connection: close\r\n\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(error403.getBytes(charset)));
	}

	private void listFilenames(String parsedURL) throws IOException {// send the list of files in
		// the directory requested
		// TODO Auto-generated method stub
		File d = new File(parsedURL);
				s.write(ByteBuffer.wrap(
						(httpVersion + " 200 OK\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Type: text/html\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Length: " + getContentLength(d) + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Date: " + sdf[0].format(new Date()) + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Connection: close\r\n\r\n").getBytes(charset)));
			if (method.equals("GET")) {
				s.write(ByteBuffer.wrap(dirList.getBytes(charset)));
			}
	}

	private int getContentLength(File d) {// returns the length of the directory
		// list and populates it
		// TODO Auto-generated method stub
		dirList = "<html><h1>Dir Contents</h1><body>";
		for (File f : d.listFiles())
			dirList += "<a href=\"" + f.getName() + "\">" + f.getName()
			+ "<br>";
		dirList += "</body></html>";
		return dirList.length();
	}

	private boolean parseRequest() throws IOException, ParseException {// parse the incoming request
		// TODO Auto-generated method stub
		// System.out.println("in parseRequest()");
				if (method.equals("TRACE") || method.equals("DELETE")
						|| method.equals("PUT") || method.equals("OPTIONS")
						|| method.equals("POST")) {
					send501error();
					return false;
				} else if (method.equals("GET") || method.equals("HEAD")) {
					// System.out.println("Filename:"+filename);
					// System.out.println("HTTP Version:"+httpVersion);
						if (req.contains("Expect: 100-Continue")
								&& httpVersion.equals("HTTP/1.1"))
							s.write(ByteBuffer.wrap(
									(httpVersion + " 100 Continue\r\n\r\n")
									.getBytes(charset)));
						else if(req.contains("Expect: 100-Continue")
								&& httpVersion.equals("HTTP/1.0"))
						{
							send417error();
							return false;
						}
					//}
					// System.out.println("Request: \n"+req);
					if (httpVersion.equals("HTTP/1.1")) {
						if (!req.contains("Host:")) {
							send400error();
							return false;
						}
					}
					if (httpVersion.equals("HTTP/1.0")
							|| httpVersion.equals("HTTP/1.1")) {
						// System.out.println("In if1");
						if (req.contains("If-Modified-Since")) {
							String modifiedDate = null;
							modifiedDate = req.substring(req
									.indexOf("If-Modified-Since:"));

							int endindex = modifiedDate.indexOf("\n");
							if (modifiedDate.charAt(endindex - 1) == '\r')
								endindex -= 2;
							else
								endindex--;
							modifiedDate = modifiedDate.substring(19,
									endindex + 1);

							if (checkIfModified(modifiedDate)
									&& method.equals("GET"))
								return true;
							else
								return false;
						} else if (req.contains("If-Unmodified-Since")) {
							String modifiedDate = null;
							modifiedDate = req.substring(req
									.indexOf("If-Unmodified-Since:"));
							int endindex = modifiedDate.indexOf("\n");
							if (modifiedDate.charAt(endindex - 1) == '\r')
								endindex -= 2;
							else
								endindex--;
							modifiedDate = modifiedDate.substring(21,
									endindex + 1);
							if (checkIfUnmodified(modifiedDate))
								return true;
							else
								return false;
						} else
							return true;
					} else {
						send505error();
						return false;
					}
				} else {
					send400error();
					return false;
				}

	}

	private void send500error() throws IOException {
		// TODO Auto-generated method stub
		String error500 = "<html>500 Internal Server Error : Server encountered an unexpected condition</html>";
				s.write(ByteBuffer.wrap(
						(httpVersion + " 500 Bad Request\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Type: text/html\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Length: " + error500.length() + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Date: " + sdf[0].format(new Date()) + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Connection: close\r\n\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(error500.getBytes(charset)));
	}

	private boolean checkIfUnmodified(String modifiedDate) throws IOException, ParseException {// check-if-unmodified
		// header
		// TODO Auto-generated method stub
		boolean isAbsolute = false;
		String reqFile = null;
			if (filename.contains("http://"))
				if (filename.contains("localhost:" + port)) {
					isAbsolute = true;
					int index = filename.indexOf(Integer.toString(port))
							+ Integer.toString(port).length();
					// System.out.println(index);
					reqFile = filename.substring(index);
					// System.out.println("\nRequested file1 : "+reqFile);
				}
			if (!isAbsolute) {
				// System.out.println("Relative path");
				char c = rootDir.charAt(rootDir.length() - 1);
				if (c == '/')
					reqFile = rootDir + filename;
				else
					reqFile = rootDir + "/" + filename;
			}
			String parsedURL = parseURL(reqFile);
			if (parsedURL.startsWith(rootDir)) {
				File f = new File(parsedURL);
				Date headerDate = null;
				Date curDate = new Date();
				boolean isValidDate = false;
				int i = 0;
				for (i = 0; i < 3; i++)
					try {
						headerDate = sdf[i].parse(modifiedDate);
						isValidDate = true;
						break;
					} catch (java.text.ParseException e) {
						continue;
					}
				if (i == 3)
					return true;
				if (isValidDate && headerDate.compareTo(curDate) < 0) {
					if (headerDate.compareTo(sdf[i].parse(sdf[i]
							.format(new Date(f.lastModified())))) < 0)// header
						// Date
						// before
						// file
						// has
						// been
						// modified
					{
						// return 412
							s.write(ByteBuffer.wrap((httpVersion + " 412 Precondition failed\r\n")
									.getBytes(charset)));
							s.write(ByteBuffer.wrap(
									("Connection: close\r\n\r\n").getBytes(charset)));
						return false;
					} else
						return true;
				} else
					// invalid date format or future date is referred. Ignore
					// the header
					return true;
			} else
				send403error();
		return false;

	}

	private boolean checkIfModified(String modifiedDate) throws IOException, ParseException {// check-if-modified
		// header
		// TODO Auto-generated method stub
		boolean isAbsolute = false;
		String reqFile = null;
			if (filename.contains("http://"))
				if (filename.contains("localhost:" + port)) {
					isAbsolute = true;
					int index = filename.indexOf(Integer.toString(port))
							+ Integer.toString(port).length();
					// System.out.println(index);
					reqFile = filename.substring(index);
					// System.out.println("\nRequested file1 : "+reqFile);
				}
			if (!isAbsolute) {
				// System.out.println("Relative path");
				char c = rootDir.charAt(rootDir.length() - 1);
				if (c == '/')
					reqFile = rootDir + filename;
				else
					reqFile = rootDir + "/" + filename;
			}
			String parsedURL = parseURL(reqFile);
			if (parsedURL.startsWith(rootDir)) {
				File f = new File(parsedURL);
				Date headerDate = null;
				Date curDate = new Date();
				boolean isValidDate = false;
				int i = 0;
				for (i = 0; i < 3; i++)
					try {
						headerDate = sdf[i].parse(modifiedDate);
						isValidDate = true;
						break;
					} catch (java.text.ParseException e) {
						continue;
					}
				if (i == 3)
					return true;
				if (isValidDate && headerDate.compareTo(curDate) < 0) {
					// System.out.println(new Date(f.lastModified()));
					if (headerDate.compareTo(sdf[i].parse(sdf[i]
							.format(new Date(f.lastModified())))) > 0)// header Date after file has
						// been modified
					{
						s.write(ByteBuffer.wrap(
								(httpVersion + " 304 Not Modified\r\n")
								.getBytes(charset)));
						s.write(ByteBuffer.wrap(
								("Date: " + sdf[0].format(new Date()) + "\r\n")
								.getBytes(charset)));
						s.write(ByteBuffer.wrap(
								("Connection: close\r\n\r\n").getBytes(charset)));
						return false;
					} else
						return true;
				} else
					// invalid date format or future date is referred. Ignore
					// the header
					return true;
			} else
				send403error();
		return false;
	}

	private void send400error() throws IOException {
		// TODO Auto-generated method stub
		String error400 = "<html>400 Error : Bad Request. Include 'Host:' header for HTTP/1.1</html>";
			s.write(ByteBuffer.wrap(
					(httpVersion + " 400 Bad Request\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Type: text/html\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Length: " + error400.length() + "\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Date: " + sdf[0].format(new Date()) + "\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(("Connection: close\r\n\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(error400.getBytes(charset)));
	}

	private void send505error() throws IOException {
		// TODO Auto-generated method stub
		String error505 = "<html>505 Error : HTTP Version not Supported.</html>";
			s.write(ByteBuffer.wrap(
					(httpVersion + " 505 HTTP Version not Supported\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Type: text/html\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Length: " + error505.length() + "\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Date: " + sdf[0].format(new Date()) + "\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(("Connection: close\r\n\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(error505.getBytes(charset)));

	}

	private void send417error() {
		// TODO Auto-generated method stub
		String error417 = "<html>417 Expectation Failed : Not met by this server</html>";
			try {
				s.write(ByteBuffer.wrap(
						(httpVersion + " 417 Expectation Failed\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Type: text/html\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Content-Length: " + error417.length() + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Date: " + sdf[0].format(new Date()) + "\r\n")
						.getBytes(charset)));
				s.write(ByteBuffer.wrap(
						("Connection: close\r\n\r\n").getBytes(charset)));
				s.write(ByteBuffer.wrap(error417.getBytes(charset)));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	private void send404error() throws IOException {
		// TODO Auto-generated method stub
		String error404 = "<html>404 Error : The requested resource doesn't exist.</html>";
			s.write(ByteBuffer.wrap(
					(httpVersion + " 404 Not Found\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Type: text/html\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Length: " + error404.length() + "\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Date: " + sdf[0].format(new Date()) + "\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(("Connection: close\r\n\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(error404.getBytes(charset)));

	}

	private void send501error() throws IOException {
		// TODO Auto-generated method stub
		String error501 = "<html>501 Error : Request Methods not implemented</html>";
			s.write(ByteBuffer.wrap(
					(httpVersion + " 501 Not Implemented\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Type: text/html\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Content-Length: " + error501.length() + "\r\n")
					.getBytes(charset)));
			s.write(ByteBuffer.wrap(
					("Date: " + sdf[0].format(new Date()) + "\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(("Connection: close\r\n\r\n").getBytes(charset)));
			s.write(ByteBuffer.wrap(error501.getBytes(charset)));
	}

}
