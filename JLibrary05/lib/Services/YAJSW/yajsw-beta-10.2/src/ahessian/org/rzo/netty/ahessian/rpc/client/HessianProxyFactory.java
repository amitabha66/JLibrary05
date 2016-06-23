package org.rzo.netty.ahessian.rpc.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
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
import org.jboss.netty.channel.SimpleChannelHandler;
import org.rzo.netty.ahessian.rpc.io.Hessian2Input;
import org.rzo.netty.ahessian.rpc.io.Hessian2Output;
import org.rzo.netty.ahessian.rpc.io.HessianInput;
import org.rzo.netty.ahessian.rpc.io.HessianOutput;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallMessage;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianRemoteObject;

/**
 * Handles client side hessian rpc proxy invocations and is a factory for
 * service proxies. <br>
 * A typical setup for a protocol in a TCP/IP socket would be: <br>
 * 
 * <pre>
 * Executor executor = ...
 * HessianProxyFactory proxyFactory = ...
 * 
 * {@link ChannelPipeline} pipeline = ...;
 * pipeline.addLast(&quot;inputStream&quot;, new InputStreamDecoder(_executor));
 * pipeline.addLast(&quot;outputStream&quot;, new OutputStreamEncoder());        
 * pipeline.addLast(&quot;hessianReplyDecoder&quot;, new HessianRPCReplyDecoder(_factory));
 * pipeline.addLast(&quot;hessianCallEncoder&quot;, new HessianRPCCallEncoder());
 * pipeline.addLast(&quot;hessianHandler&quot;, proxyFactory);
 * </pre>
 * 
 * <br>
 * Typical usage within the client would be:
 * 
 * <pre>
 * 
 * ClientBootstrap bootstrap = ...
 * ChannelPipelineFactory pipelinetFactory = new ...(proxyFactory)
 * bootstrap.setPipelineFactory(...)
 * bootstrap.connect(...)
 * 
 * // get a service proxy 
 * Map options = new HashMap();
 * options.put(&quot;id&quot;, &quot;myServiceName&quot;);
 * // AsynchMyServiceInterface is an interface including the same methods as MyServiceInterface 
 * //except that the return type is always of type HessianProxyFuture
 * AsynchMyServiceInterface service = (AsynchMyServiceInterface) factory.create(AsynchMyServiceInterface.class, getClassLoader(), options);
 * 
 * // invoke a service method
 * HessianProxyFuture future = service.myMethod();
 * // wait for the result
 * // if an exception is thrown by the server the exception is thrown by the call to the get() method 
 * Object result = future.get();
 * </pre>
 */

@ChannelPipelineCoverage("all")
public class HessianProxyFactory extends SimpleChannelHandler
{
	private Map<Integer, Future<Object>>					_openCalls		= Collections.synchronizedMap(new HashMap<Integer, Future<Object>>());
	private volatile int									_id				= 0;
	private volatile Channel								_channel		= null;
	private com.caucho.hessian.client.HessianProxyFactory	_factory		= null;
	private LinkedBlockingQueue<HessianRPCCallMessage>		_pendingCalls	= new LinkedBlockingQueue<HessianRPCCallMessage>();

	/** The _done listener. */
	Runnable												_doneListener;

	/** The _executor. */
	Executor												_executor;
	private Lock											_lock			= new ReentrantLock();
	private Condition										_connected		= _lock.newCondition();

	/** The _stop. */
	boolean													_stop			= false;
	private String _name;

	/**
	 * Instantiates a new hessian proxy factory.
	 * 
	 * @param executor
	 *            the executor
	 */
	public HessianProxyFactory(Executor executor, String name)
	{
		this(executor, name, null);
	}

	public HessianProxyFactory(Executor executor, String name, ClassLoader loader)
	{
		_executor = executor;
		_name = name;
		if (loader == null)
			_factory = new com.caucho.hessian.client.HessianProxyFactory();
		else
			_factory = new com.caucho.hessian.client.HessianProxyFactory(loader);
		_executor.execute(new Runnable()
		{
			public void run()
			{
				HessianRPCCallMessage message = null;
				while (!_stop)
				{
					// if previous message sent
					if (message == null)
						try
						{
							message = _pendingCalls.take();
							// System.out.println("sender "+message.getHeaders().get("call-id"));
						}
						catch (InterruptedException e1)
						{
							e1.printStackTrace();
						}
					if (message == null)
						continue;
					if (_stop)
						break;
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
						if (!_stop && message != null && message.getMethod() != null)
						{
					ChannelFuture future = _channel.write(message);
					// future.awaitUninterruptibly();
					// if (future.isSuccess())
						}
					message = null;
				}
			}
		});
	}

	/**
	 * Gets the hessian2 input.
	 * 
	 * @param is
	 *            the is
	 * 
	 * @return the hessian2 input
	 */
	public AbstractHessianInput getHessian2Input(InputStream is)
	{
		return new Hessian2Input(is);
	}

	/**
	 * Gets the hessian input.
	 * 
	 * @param is
	 *            the is
	 * 
	 * @return the hessian input
	 */
	public AbstractHessianInput getHessianInput(InputStream is)
	{
		return new HessianInput(is);
	}

	/**
	 * Gets the hessian output.
	 * 
	 * @param out
	 *            the out
	 * 
	 * @return the hessian output
	 */
	public HessianOutput getHessianOutput(OutputStream out)
	{
		HessianOutput out1 = new HessianOutput(out);
		out1.setSerializerFactory(_factory.getSerializerFactory());
		return out1;
	}

	public AbstractHessianOutput getHessian2Output(OutputStream out)
	{
		Hessian2Output out2 = new Hessian2Output(out);
		out2.setSerializerFactory(_factory.getSerializerFactory());
		return out2;
	}

	/**
	 * Checks if is overload enabled.
	 * 
	 * @return true, if is overload enabled
	 */
	public boolean isOverloadEnabled()
	{
		return _factory.isOverloadEnabled();
	}

	/**
	 * Send request.
	 * 
	 * @param methodName
	 *            the method name
	 * @param args
	 *            the args
	 * @param options
	 *            the options
	 * 
	 * @return the future< object>
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	synchronized Future<Object> sendRequest(String methodName, Object[] args, Map options) throws InterruptedException
	{
		Map<String, Object> headers = options;
		headers.put("call-id", _id);
		HessianRPCCallMessage message = new HessianRPCCallMessage(methodName, args, headers);
		Future<Object> future = new HessianProxyFuture();
		_openCalls.put(new Integer(_id), future);
		// System.out.println("sending "+_id);
		_id++;
		_pendingCalls.put(message);
		return future;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss
	 * .netty.channel.ChannelHandlerContext,
	 * org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
	{
		if (e.getMessage() instanceof HessianRPCReplyMessage)
		{
			HessianRPCReplyMessage message = (HessianRPCReplyMessage) e.getMessage();
			Map<String, Object> headers = message.getHeaders();
			if (headers != null)
			{
				Integer id = (Integer) headers.get("call-id");
				if (id != null)
				{
					// System.out.println("received "+id);
					HessianProxyFuture future = (HessianProxyFuture) _openCalls.get(id);
					if (headers.get("completed") == null || Boolean.TRUE.equals(headers.get("completed")))
						_openCalls.remove(id);
					if (_doneListener != null && _openCalls.isEmpty())
						_doneListener.run();
					if (future != null)
						future.set(message);
					else
						log("no future for call reply " + id + " " + message.getValue());
				}
				else
					log("message missing id " + message);

			}
			else
				log("message missing headers " + message);
		}
		ctx.sendUpstream(e);
	}

	private void log(String txt)
	{
		System.out.println(txt);
	}

	/**
	 * Creates a service proxy.
	 * 
	 * @param api
	 *            the "asynched" api of the service
	 * @param loader
	 *            the class loader for creating the proxy
	 * @param options
	 *            the options
	 * 
	 * @return the object
	 */
	public Object create(Class api, ClassLoader loader, Map options)
	{
		if (api == null)
			throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
		InvocationHandler handler = null;

		handler = new AsyncHessianProxy(this, api, options);
		if (options.get("sync") != null)
			handler = new SyncHessianProxy(handler);

		return Proxy.newProxyInstance(loader, new Class[]
		{ api, HessianRemoteObject.class }, handler);
	}

	/**
	 * Gets the channel.
	 * 
	 * @return the channel
	 */
	Channel getChannel()
	{
		return _channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss
	 * .netty.channel.ChannelHandlerContext,
	 * org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		_lock.lock();
		_channel = ctx.getChannel();
		super.channelConnected(ctx, e);
		_connected.signal();
		_lock.unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.netty.channel.SimpleChannelHandler#channelDisconnected(org.
	 * jboss.netty.channel.ChannelHandlerContext,
	 * org.jboss.netty.channel.ChannelStateEvent)
	 */
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		_channel = null;
		_stop = true;
		_lock.lock();
		_connected.signal();
		_lock.unlock();
		// put something in the queue in case the worker thread hangs in _pendingCalls.take()
		_pendingCalls.offer(new HessianRPCCallMessage(null, null, null));
		super.channelDisconnected(ctx, e);
	}

	/**
	 * Sets the done listener. This listener is fired whenever all requests have been completed
	 * 
	 * @param listener
	 *            the new listener
	 */
	public void setDoneListener(Runnable listener)
	{
		_doneListener = listener;
	}
	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    	System.out.println("error accessing " + _name);
    	ctx.getChannel().disconnect();
        ctx.getChannel().close();
        if (!_stop)
        {
    		_channel = null;
		_stop = true;
		_lock.lock();
		_connected.signal();
		_lock.unlock();
		// put something in the queue in case the worker thread hangs in _pendingCalls.take()
		_pendingCalls.offer(new HessianRPCCallMessage(null, null, null));
        }

    }


}
