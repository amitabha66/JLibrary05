package org.rzo.netty.ahessian.rpc.server;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallMessage;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;
import org.rzo.netty.ahessian.session.ServerSessionFilter;

import java.lang.reflect.Method;

/**
 * Wraps an object as a {@link Service}. Methods are invoked as soon as they are received.
 * Invocation and return of result are executed within the netty worker thread. <br>
 * This type of service is used for short running invocations.
 * <br>
 * Typical usage:
 * <pre>
 * 
 * // the object to be wrapped, implements MyServiceInterface
 * Object myServiceObject; 
 * 
 * // the netty rpc service handler
 * HessianRPCServiceHandler handler;
 * 
 * Service myService = new ImmediateInvokeService(myServiceObject, MyServiceInterface.class);
 * 
 * // Clients will access the service through the given name
 * handler.addService("myServiceName", myService);
 * 
 * </pre>
 */
public class ImmediateInvokeService extends HessianSkeleton
{
	public static ThreadLocal threadLocalSession = new ThreadLocal();

	/**
	 * Instantiates a new immediate invoke service.
	 * 
	 * @param service the service object implementing apiClass
	 * @param apiClass the api of the service exposed to the client
	 * @param factory the netty handler
	 */
	public ImmediateInvokeService(Object service, Class apiClass, HessianRPCServiceHandler factory)
	{
		super(service, apiClass, factory);
	}

	/* (non-Javadoc)
	 * @see org.rzo.netty.ahessian.rpc.server.HessianSkeleton#messageReceived(org.rzo.netty.ahessian.rpc.HessianRPCCallMessage)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, HessianRPCCallMessage message)
	{
		threadLocalSession.set(ServerSessionFilter.getSession(ctx));
		invoke(message);
	}
	
	/**
	 * Invokes the RPC call and sends back the result
	 * 
	 * @param message the message
	 */
	 void invoke(HessianRPCCallMessage message)
	{
		Object result = null;
		Object fault = null;
		try
		{
			Method method = getMethod(message);
			result = method.invoke(_service, message.getArgs());
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
			fault = ex;
		}
		writeResult(new HessianRPCReplyMessage(result, fault, message.getHeaders(), null));
	}
	

}
