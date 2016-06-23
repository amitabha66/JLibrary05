package org.rzo.netty.ahessian.example.io;

import org.rzo.netty.ahessian.example.log.OutLogger;
import org.rzo.netty.ahessian.io.InputStreamDecoder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class InputServer
{
    public static void main(String[] args)
    {
        Executor executor = Executors.newCachedThreadPool();

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                		executor,
                		executor));
        

        // Set up the event pipeline factory.
        ClientSocketChannelFactory cf =
            new NioClientSocketChannelFactory(executor, executor);
        bootstrap.getPipeline().addLast("logger",new OutLogger("1"));
        // InputStreamDecoder returns an input stream and calls the next handler in a separate thread
        bootstrap.getPipeline().addLast("inputStream", new InputStreamDecoder(executor));
        // read line from the input stream and print it
        // close the connection on "bye"
        bootstrap.getPipeline().addLast("printHandler", new SimpleChannelUpstreamHandler()
        {
        	@Override
            public void messageReceived(
                    ChannelHandlerContext ctx, MessageEvent e) throws Exception 
                    {
        				InputStream in = (InputStream) e.getMessage();
        				BufferedReader  b = new BufferedReader(new InputStreamReader(in));
        				String line = "";
        				while ( (line = b.readLine()) != null)
        				{
        					System.out.println(line);
        					if ("bye".equals(line))
        						ctx.getChannel().close();
        					if ("quit".equals(line))
        					{
        						ctx.getChannel().close();
        						System.exit(0);
        					}
        				}
        				
        				System.out.println("done!!");
                    }
        	
        });

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(8080));

    }

}
