package org.rzo.netty.ahessian.rpc.message;

import java.util.Map;

import org.jboss.netty.channel.Channel;

/**
 * reply message for a remote invocation
 */
public class HessianRPCReplyMessage
{
	
	/** The _value. */
	Object _value;
	
	/** The _fault. */
	Throwable _fault;
	
	/** The _headers. */
	Map<String, Object> _headers;
	
	/** The _channel. */
	Channel _channel;
	
	/**
	 * Instantiates a new hessian rpc reply message.
	 * 
	 * @param value the value
	 * @param fault the fault
	 * @param headers the headers
	 * @param channel the channel
	 */
	public HessianRPCReplyMessage(Object value, Object fault, Map headers, Channel channel)
	{
		_value = value;
		_fault = (Throwable) fault;
		_headers = headers;
		_channel = channel;
	}

	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public Object getValue()
	{
		return _value;
	}

	/**
	 * Gets the fault.
	 * 
	 * @return the fault
	 */
	public Throwable getFault()
	{
		return _fault;
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

}
