package org.rzo.netty.ahessian.rpc.message;

import java.util.Map;

import org.jboss.netty.channel.Channel;


/**
 * message used for requesting a remote invocation
 */
public class HessianRPCCallMessage
{
	
	/** The _method. */
	String _method;
	
	/** The _args. */
	Object[] _args;
	
	/** The _headers. */
	Map<String, Object> _headers;
	
	/** The _channel. */
	Channel _channel;
	
	/**
	 * Gets the channel.
	 * 
	 * @return the channel
	 */
	public Channel getChannel()
	{
		return _channel;
	}


	/**
	 * Sets the channel.
	 * 
	 * @param channel the new channel
	 */
	public void setChannel(Channel channel)
	{
		_channel = channel;
	}


	/**
	 * Gets the headers.
	 * 
	 * @return the headers
	 */
	public Map<String, Object> getHeaders()
	{
		return _headers;
	}


	/**
	 * Instantiates a new hessian rpc call message.
	 * 
	 * @param method the method
	 * @param args the args
	 * @param headers the headers
	 */
	public HessianRPCCallMessage(String method, Object[] args, Map<String, Object> headers)
	{
		_method = method;
		_args = args;
		_headers = headers;
	}


	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod()
	{
		return _method;
	}

	/**
	 * Gets the args.
	 * 
	 * @return the args
	 */
	public Object[] getArgs()
	{
		return _args;
	}

}
