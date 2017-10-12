package edu.upenn.cis455.webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class HttpServer_event {

	static int port;
	static String rootDir;
	static boolean shutDown;
	public static void main(String args[])
	{
		if(args.length<2)
		{
			System.out.println("*** Author: Kushmitha Unnikumar (kushm)");
			return;
		}
		port=Integer.parseInt(args[0]);
		rootDir=args[1];
		shutDown = false;
		Selector s=null;
		ServerSocketChannel serSock=null;
		SocketChannel sock=null;
		try {
			serSock = ServerSocketChannel.open();
			serSock.configureBlocking(false);
			serSock.socket().bind(new InetSocketAddress(port));
			s = Selector.open();
			serSock.register(s,SelectionKey.OP_ACCEPT);

		} catch (IOException e) {
			shutDown=true;
		}

		while(!shutDown)
		{
			System.out.println("Server on");
			try 
			{
				while(s.select()>0)
				{
					Set<SelectionKey> keys= s.selectedKeys();
					Iterator<SelectionKey> keyIterator = keys.iterator();
					while(keyIterator.hasNext())
					{
						SelectionKey key = keyIterator.next();
						if(key.isAcceptable())
						{
							serSock = (ServerSocketChannel)key.channel();
							sock= serSock.accept();
							sock.configureBlocking(false);
							sock.register(s,SelectionKey.OP_READ);
						}
						else if(key.isReadable())
						{
							sock=(SocketChannel)key.channel();
							sock.configureBlocking(false);
							ByteBuffer requestBuffer = ByteBuffer.allocate(4096);
							sock.read(requestBuffer);
							requestBuffer.flip(); //converts from writing mode to reading mode
							CharBuffer request = Charset.forName("UTF-8").newDecoder().decode(requestBuffer);
							RequestHandling_Event handler = new RequestHandling_Event(port,rootDir,sock,request.toString(),Charset.forName("UTF-8"));
							shutDown=handler.processRequest();
							sock.close();
							if(shutDown)	//check if a shut down was requested
							{
								System.exit(1);
							}
						}
						keyIterator.remove();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}

	}

}