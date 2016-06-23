package org.rzo.netty.ahessian.example.io;

import org.rzo.netty.ahessian.example.log.OutLogger;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

public class OutputClient
{
    public static void main(String[] args)
    {
        Executor executor = Executors.newCachedThreadPool();

        // Configure the client.
        ClientBootstrap bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                		executor,
                		executor));
        

        // Set up the event pipeline factory.
        ClientSocketChannelFactory cf =
            new NioClientSocketChannelFactory(executor, executor);
        // InputStreamDecoder returns an input stream and calls the next handler in a separate thread
        bootstrap.getPipeline().addLast("logger",new OutLogger("1"));
        bootstrap.getPipeline().addLast("outputStream", new OutputStreamEncoder());
        // read line from the input stream and print it
        // close the connection on "bye"
        bootstrap.getPipeline().addLast("writeHandler", new SimpleChannelUpstreamHandler()
        {
        	@Override
            public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                ctx.sendUpstream(e);
        		OutputStream out = (OutputStream) ctx.getPipeline().getContext("outputStream").getAttachment();
        		PrintWriter p = new PrintWriter(out);
        		try
        		{
        			for (int i=0; i<10; i++)
        			{
        				p.println("abc");
        				p.flush();
        			}
        			p.println("quit");
        			p.flush();
        		}
        		catch (Exception ex)
        		{
        			ex.printStackTrace();
        		}
        	}
        	
            public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                ctx.sendUpstream(e);
                System.exit(0);
            }

        	
        });

        // Start the connection attempt.
        ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 8080));

    }

}
