package org.rzo.netty.ahessian.example.chat.client;

import org.rzo.netty.ahessian.rpc.client.HessianProxyFactory;
import org.rzo.netty.ahessian.rpc.client.HessianProxyFuture;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

public class ChatClient
{
    public static void main(String[] args) throws InterruptedException
    {
	
    ExecutorService executor = Executors.newCachedThreadPool();

    // Configure the client.
    ClientBootstrap bootstrap = new ClientBootstrap(
            new NioClientSocketChannelFactory(
            		executor,
            		executor));
    
    HessianProxyFactory factory = new HessianProxyFactory(executor, "localhost:8080");
    bootstrap.setPipelineFactory(
            new ChatClientSessionPipelineFactory(new ChatClientMixinPipelineFactory(executor, factory)));


    
     // Start the connection attempt.
    ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 8080));
    // Wait until the connection attempt succeeds or fails.
    Channel channel = future.awaitUninterruptibly().getChannel();
    if (future.isSuccess())
    	System.out.println("connected");
    
    // get a proxy 
    Map options = new HashMap();
    AsynchChatService service = (AsynchChatService) factory.create(AsynchChatService.class, ChatClient.class.getClassLoader(), options);

    // logon to the chat server with current time as user name
    service.logon(""+System.currentTimeMillis());
    
    // request a getMessage continuation
    final HessianProxyFuture f = (HessianProxyFuture) service.getMessage();
    
    // add a listener for incoming messages
    f.addListener(new Runnable()
    {
    	public void run()
    	{
    	try
		{
			System.out.println(f.get());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
    	}
    });
    
    // send some messages
    for (int i=0; i<1000; i++)
    {
    	//Thread.sleep(5000);
    	service.putMessage("this is message "+i);
    }
    
    // send a last message
    service.putMessage("bye");
    
    // done
    service.logoff();
    }

}
