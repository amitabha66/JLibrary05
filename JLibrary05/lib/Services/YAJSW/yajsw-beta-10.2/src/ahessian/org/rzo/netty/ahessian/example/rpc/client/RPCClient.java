package org.rzo.netty.ahessian.example.rpc.client;

import org.rzo.netty.ahessian.rpc.client.HessianProxyFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

public class RPCClient
{
    public static void main(String[] args)
    {
	
    ExecutorService executor = Executors.newCachedThreadPool();

    // Configure the client.
    ClientBootstrap bootstrap = new ClientBootstrap(
            new NioClientSocketChannelFactory(
            		executor,
            		executor));
    
    HessianProxyFactory factory = new HessianProxyFactory(executor, "localhost:8080");
    bootstrap.setPipelineFactory(
            new RPCClientSessionPipelineFactory(new RPCClientMixinPipelineFactory(executor, factory)));


    
     // Start the connection attempt.
    ChannelFuture future = bootstrap.connect(new InetSocketAddress("localhost", 8080));
    // Wait until the connection attempt succeeds or fails.
    Channel channel = future.awaitUninterruptibly().getChannel();
    if (future.isSuccess())
    	System.out.println("connected");
    
    // get a proxy 
    Map options = new HashMap();
    AsynchHelloWorldServiceInterface service = (AsynchHelloWorldServiceInterface) factory.create(AsynchHelloWorldServiceInterface.class, RPCClient.class.getClassLoader(), options);
    final int amount = 100000;

    final long t = System.currentTimeMillis();

    factory.setDoneListener(new Runnable()
    {
		public void run()
		{
			double d = ((double)(System.currentTimeMillis()-t))/1000.0;
			System.out.println("all done "+d + " calls/s "+(double)amount/(d));
			System.exit(0);
		}    	
    });


    
    for (int i = 0; i < amount; i++)
    {
    	service.HelloWorld(""+i);
    	try
		{
			//System.out.println(fpFuture.get());
			//Thread.sleep(100);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }
    System.out.println("done sending "+ (System.currentTimeMillis()-t));
    // wait for some of the results
//    try
//	{
//		Thread.sleep(2000);
//	}
//	catch (InterruptedException e1)
//	{
//		// TODO Auto-generated catch block
//		e1.printStackTrace();
//	}
    //disconnect from the server and wait
    //channel.close();
//    try
//	{
//		//Thread.sleep(5000);
//	}
//	catch (InterruptedException e)
//	{
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//	//System.out.println("reconnect");
//	// reconnect to the server and get the remaining results
//     //future = bootstrap.connect(new InetSocketAddress("localhost", 8080));
//    // Wait until the connection attempt succeeds or fails.
//     //future.awaitUninterruptibly();
//    if (future.isSuccess())
//    	System.out.println("connected");
//
    }

}
