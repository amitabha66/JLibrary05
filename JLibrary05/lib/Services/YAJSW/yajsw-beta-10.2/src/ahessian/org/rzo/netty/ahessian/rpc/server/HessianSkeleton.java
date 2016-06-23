package org.rzo.netty.ahessian.rpc.server;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallMessage;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;

import java.lang.reflect.Method;

/**
 * The Class HessianSkeleton, extends the original HessianSkeleton, 
 * so that it can be used in a non-servlet environment
 * 
 * TODO method name overloading. currently only overloading by number of arguments is supported
 */
public abstract class HessianSkeleton extends com.caucho.hessian.server.HessianSkeleton implements Service
{
	
	/** The _service. */
	Object _service;
	
	/** The _factory. */
	HessianRPCServiceHandler _factory;
	
	/**
	 * Instantiates a new hessian skeleton.
	 * 
	 * @param service the service
	 * @param apiClass the api class
	 * @param factory the factory
	 */
	public HessianSkeleton(Object service, Class apiClass, HessianRPCServiceHandler factory)
	{
	    super(apiClass);
		_service = service;
		_factory = factory;
	}
	
	
	/**
	 * Gets the method.
	 * 
	 * @param message the message
	 * 
	 * @return the method
	 */
	public Method getMethod(HessianRPCCallMessage message)
	{
	    Method method = getMethod(message.getMethod() + "__" + message.getArgs().length);
	    if (method == null)
	      method = getMethod(message.getMethod());	    
	    return method;
	}
	
	/**
	 * Write result.
	 * 
	 * @param message the message
	 */
	public void writeResult(HessianRPCReplyMessage message)
	{
		_factory.writeResult(message);
	}
	
	/* (non-Javadoc)
	 * @see org.rzo.netty.ahessian.rpc.server.Service#messageReceived(org.rzo.netty.ahessian.rpc.HessianRPCCallMessage)
	 */
	abstract public void messageReceived(ChannelHandlerContext ctx, HessianRPCCallMessage message);

}
