package org.rzo.netty.ahessian.rpc.client;

import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

import org.jboss.netty.channel.Channel;

import com.caucho.services.server.AbstractSkeleton;

/**
 * A proxy object implementing asynchronous invocations.
 * All invocations return a HessianProxyFuture
 */
class AsyncHessianProxy implements InvocationHandler
{
	
	  private WeakHashMap<Method,String> _mangleMap = new WeakHashMap<Method,String>();
	  private HessianProxyFactory _factory;
	  private Class _api;
	  private Map _options;

	  
	/**
	 * Instantiates a new async hessian proxy.
	 * 
	 * @param factory the factory
	 * @param api the api
	 * @param options the options
	 */
	AsyncHessianProxy(HessianProxyFactory factory, Class api, Map options)
	{
		_factory = factory;
		_api = api;
		_options = options;
	}
	  
	  /**
  	 * Gets the channel.
  	 * 
  	 * @return the channel
  	 */
  	Channel getChannel()
	  {
		  return _factory.getChannel();
	  }

	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
		  public Object invoke(Object proxy, Method method, Object []args)
		    throws Throwable
		  {
		    String mangleName;
		    Channel channel = getChannel();

		    synchronized (_mangleMap) {
		      mangleName = _mangleMap.get(method);
		    }

		    if (mangleName == null) {
		      String methodName = method.getName();
		      Class []params = method.getParameterTypes();

		      // equals and hashCode are special cased
		      if (methodName.equals("equals")
			  && params.length == 1 && params[0].equals(Object.class)) {
			Object value = args[0];
			if (value == null || ! Proxy.isProxyClass(value.getClass()))
			  return Boolean.FALSE;

			Object proxyHandler = Proxy.getInvocationHandler(value);

			if (! (proxyHandler instanceof AsyncHessianProxy))
			  return Boolean.FALSE;
			
			AsyncHessianProxy handler = (AsyncHessianProxy) proxyHandler;

			return new Boolean(this.equals(handler));
		      }
		      else if (methodName.equals("hashCode") && params.length == 0)
			return immediateFuture(new Integer(this.hashCode()));
		      else if (methodName.equals("getHessianType"))
			return immediateFuture(proxy.getClass().getInterfaces()[0].getName());
		      else if (methodName.equals("getHessianURL"))
			return immediateFuture(channel == null ? "?" : channel.toString());
		      else if (methodName.equals("toString") && params.length == 0)
			return immediateFuture("HessianProxy[" + this + "]");
		      
		      if (! _factory.isOverloadEnabled())
			mangleName = method.getName();
		      else
		        mangleName = mangleName(method);

		      synchronized (_mangleMap) {
			_mangleMap.put(method, mangleName);
		      }
		    }
		          return sendRequest(mangleName, args);

	}
		  
		  protected Future<Object> sendRequest(String methodName, Object []args)
		    throws InterruptedException
		  {
			  
			 return _factory.sendRequest(methodName, args, new HashMap(_options));
		  }
		  
		  /** The id. */
  		static volatile int id = 0;
		  
		  protected Map getHeaders()
		  {
			  Map result = new HashMap();
			  result.put("call-id", id++);
			  return result;
			  
		  }
		  
		  
		  protected String mangleName(Method method)
		  {
		    Class []param = method.getParameterTypes();
		    
		    if (param == null || param.length == 0)
		      return method.getName();
		    else
		      return AbstractSkeleton.mangleName(method, false);
		  }
		  
		  private Future immediateFuture(Object result)
		  {
			  HessianProxyFuture future = new HessianProxyFuture();
			  future.set(new HessianRPCReplyMessage(result, null, null, null));
			  return future;
		  }




}
