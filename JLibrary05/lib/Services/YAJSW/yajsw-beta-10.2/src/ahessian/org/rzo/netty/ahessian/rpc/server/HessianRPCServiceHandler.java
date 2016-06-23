package org.rzo.netty.ahessian.rpc.server;

import org.rzo.netty.ahessian.rpc.message.HessianRPCCallDecoder;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallMessage;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyEncoder;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;
import org.rzo.netty.ahessian.io.InputStreamDecoder;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handles server side hessian rpc calls. 
 * <br>
 * A typical setup for a protocol in a TCP/IP socket would be:
 * 
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
  *  pipeline.addLast("inputStream", new InputStreamDecoder(_executor));
  *  pipeline.addLast("outputStream", new OutputStreamEncoder());
  *  pipeline.addLast("callDecoder", new HessianRPCCallDecoder());
  *  pipeline.addLast("replyEncoder", new HessianRPCReplyEncoder());
  *  HessianRPCServiceHandler handler =  new HessianRPCServiceHandler(executor);
  *  {@link Service} service = ...
  *  {@link Executor} executor = ...
  *  factory.addService("default", new ContinuationService(service, ServiceApi.class, handler, executor));
  *  pipeline.addLast("hessianRPCServer", handler);
 * </pre>
 */
@ChannelPipelineCoverage("one")
public class HessianRPCServiceHandler extends SimpleChannelUpstreamHandler
{
	
	/** maps service names to services. */
	private Map<String, HessianSkeleton> _services = new HashMap<String, HessianSkeleton>();
	/** queue of pending replies. */
	private LinkedBlockingQueue<HessianRPCReplyMessage> _pendingReplies = new LinkedBlockingQueue<HessianRPCReplyMessage>();
	private Lock			_lock				= new ReentrantLock();
	/** if the client disconnects results are queued. They are sent once connection has been reestablished */
	private Condition		_connected			= _lock.newCondition();
	
	/** thread pool to get a thread to send the replies */
	private Executor _executor;
	
	/** TODO  indicates that execution should be stopped */
	private boolean _stop = false;
	
	/** TODO channel on which to send replies, or should we use the context ? */
	Channel _channel;

	
	/**
	 * Instantiates a new hessian rpc service handler.
	 * 
	 * @param executor the thread pool to get a thread to send replies
	 */
	public HessianRPCServiceHandler(Executor executor)
	{
		_executor = executor;
		_executor.execute(new Runnable()
		{
			public void run()
			{
				HessianRPCReplyMessage message = null;
				while (!_stop)
				{
					// if previous message sent 
					if (message == null)
						try
						{
							message = _pendingReplies.take();
							//System.out.println("sender "+message.getHeaders().get("call-id"));
						}
						catch (InterruptedException e1)
						{
							e1.printStackTrace();
						}
						if (message == null)
							continue;
					_lock.lock();
					while (_channel == null || !_channel.isConnected() && !_stop)
						try
						{
							_connected.await();
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
						_lock.unlock();
					ChannelFuture future = _channel.write(message);
						//future.awaitUninterruptibly();
					//if (future.isSuccess())
						message = null;					
				}
			}
		});
	}
	
	/**
	 * Adds a service to the handler.
	 * 
	 * @param name the name of the service
	 * @param service the service wrapper
	 */
	public void addService(String name, HessianSkeleton service)
	{
		_services.put(name, service);
	}
	
	/**
	 * Removes a service.
	 * 
	 * @param name the name
	 */
	public void removeService(String name)
	{
		_services.remove(name);
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) throws Exception 
    {
		HessianRPCCallMessage message = (HessianRPCCallMessage) e.getMessage();
		//System.out.println("invoke "+message.getHeaders().get("call-id"));
		message.setChannel(ctx.getChannel());
		HessianSkeleton service = getService(message);
		service.messageReceived(ctx, message);
    }

	private HessianSkeleton getService(HessianRPCCallMessage message) 
	{
		String id = (String) message.getHeaders().get("id");
		if (id == null)
			id = "default";
		return _services.get(id);
	}

    void writeResult(HessianRPCReplyMessage message)
	{
		try
		{
			_pendingReplies.put(message);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	_lock.lock();
        _channel = ctx.getChannel();
        super.channelOpen(ctx, e);
        _connected.signal();
        _lock.unlock();
    }
    
    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
     */
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	_lock.lock();
        _channel = null;
        super.channelClosed(ctx, e);
        _lock.unlock();
   }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        ctx.getChannel().close();
    }

    
    

	
	


}
