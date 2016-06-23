package org.rzo.netty.ahessian.rpc.message;

import org.rzo.netty.ahessian.rpc.client.HessianProxyFactory;
import org.rzo.netty.ahessian.rpc.io.Hessian2Input;
import org.rzo.netty.ahessian.rpc.io.HessianInput;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.HessianProtocolException;

/**
 * reads a reply message from an input stream
 */
@ChannelPipelineCoverage("one")
public class HessianRPCReplyDecoder extends SimpleChannelUpstreamHandler
{

	/** The _factory. */
	HessianProxyFactory _factory;
	
	/**
	 * Instantiates a new hessian rpc reply decoder.
	 * 
	 * @param factory the factory
	 */
	public HessianRPCReplyDecoder(HessianProxyFactory factory)
	{
		_factory = factory;
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent evt) throws Exception 
    {
		InputStream inx = (InputStream) evt.getMessage();
	    Hessian2Input in = (Hessian2Input) _factory.getHessian2Input(inx);

			while (ctx.getChannel().isConnected())
			{
				Channels.fireMessageReceived(ctx, parseReply(in));
				in.resetReferences();
			 }
    }
	
	/**
	 * Parses the reply.
	 * 
	 * @param is the is
	 * 
	 * @return the hessian rpc reply message
	 */
	HessianRPCReplyMessage parseReply(Hessian2Input in)
	{
  		Map headers = new HashMap();
  		Object value = null;
  		Object fault = null;
		int code;
		try
		{
		if ((code=in.read()) != 'H')
		{
	    	  return new HessianRPCReplyMessage(null, new HessianProtocolException("'" + (char) code + "' is an unknown code"), null, null);
		}
		in.read();
		in.read();
		in.readEnvelope();
		String h = in.readString();
		if (!"Header".equals(h))
		{
	    	  return new HessianRPCReplyMessage(null, new HessianProtocolException("Missing headers"), null, null);
		}
	    int l = in.readInt();
	    for (int i=0; i<l; i++) 
	    {
	      String header = in.readString();
	      Object hvalue = in.readObject();
	      //System.out.println("value "+value);
	      headers.put(header, hvalue);
	    }
		if ((code=in.read()) != 'H')
		{
	    	  return new HessianRPCReplyMessage(null, new HessianProtocolException("'" + (char) code + "' is an unknown code"), null, null);
		}
		in.read();
		in.read();

		Object obj = in.readReply(null);
		in.completeReply();
		in.completeEnvelope();

		if (obj instanceof Throwable)
			return new HessianRPCReplyMessage(null, obj, headers, null);
		else
			return new HessianRPCReplyMessage(obj, null, headers, null);
		
			

	    
		}
		catch (Throwable ex)
		{
			return new HessianRPCReplyMessage(null, ex, null, null);
		}


	}
	      
      	/**
      	 * Parses the hessian2 reply.
      	 * 
      	 * @param is the is
      	 * 
      	 * @return the hessian rpc reply message
      	 */
      	HessianRPCReplyMessage parseHessian2Reply(InputStream is)
	      {

		    Hessian2Input in;

	  		in = (Hessian2Input) _factory.getHessian2Input(is);

	  		Map headers = null;
	  		Object value = null;
	  		Object fault = null;
	  		try
	  		{
		  		int major = is.read();
		  		int minor = is.read();

	  			headers = in.readHeaders();
	  			value = in.readReply(Class.forName((String)headers.get("return-type")));
	  		}
	  		catch (Throwable ex)
	  		{
	  			fault = ex;
	  		}

	  		return new HessianRPCReplyMessage(value, fault, headers, null);
	  	      
	      }
	      
	      /**
      	 * Parses the hessian reply.
      	 * 
      	 * @param is the is
      	 * 
      	 * @return the hessian rpc reply message
      	 */
      	HessianRPCReplyMessage parseHessianReply(InputStream is)
	      {
			    Hessian2Input in;
	    			in = (Hessian2Input) _factory.getHessian2Input(is);

	    	  		Map headers = null;
	    	  		Object value = null;
	    	  		Object fault = null;
	    	  		try
	    	  		{
		    			int major = is.read();
		    			int minor = is.read();
		    			
		    			in.startReplyBody();
	    	  			headers = in.readHeaders();
	    	  			value = in.readObject();
		    			in.completeReply();
	    	  		}
	    	  		catch (Throwable ex)
	    	  		{
	    	  			fault = ex;
	    	  		}

	    	  		return new HessianRPCReplyMessage(value, fault, headers, null);
	      }
    
	

}
