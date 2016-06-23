package org.rzo.netty.ahessian.rpc.message;

import org.rzo.netty.ahessian.rpc.io.Hessian2Input;
import org.rzo.netty.ahessian.rpc.io.HessianInput;
import org.rzo.netty.ahessian.rpc.server.HessianRPCServiceHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 *  Decodes a call request from an input stream
 */
@ChannelPipelineCoverage("one")
public class HessianRPCCallDecoder extends SimpleChannelUpstreamHandler
{
	
	/** The _factory. */
	HessianRPCServiceHandler _factory;
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) throws Exception 
    {
		InputStream inx = (InputStream) e.getMessage();
		Hessian2Input in = new Hessian2Input(inx);
		while (ctx.getChannel().isConnected())
		{
		try
		{
		Map<String, Object> headers = new HashMap<String, Object>();
		String methodName = null;
		List values = new ArrayList();
	
		int ch;
		if ((ch=in.read()) != 'H')
		{
			System.out.println("H expected got " + "0x" + Integer.toHexString(ch & 0xff) + " (" + (char) + ch + ")");
			continue;
		}
		in.read();
		in.read();
		in.readEnvelope();
		String h = in.readString();
		if (!"Header".equals(h))
		{
			System.out.println("missing header");
			continue;
		}
	    int l = in.readInt();
	    for (int i=0; i<l; i++) 
	    {
	      String header = in.readString();
	      Object value = in.readObject();
	      //System.out.println("value "+value);
	      headers.put(header, value);
	    }
	    in.readCall();
	    methodName = in.readMethod();
	    int argsLength = in.readInt();

	   for (int i=0; i<argsLength; i++)
	    	values.add(in.readObject());
	   
	   in.completeCall();
	   in.completeEnvelope();

	    HessianRPCCallMessage result = new HessianRPCCallMessage(methodName, values.toArray(), headers);
	    //System.out.println("received " + headers.get("call-id"));
	    Channels.fireMessageReceived(ctx, result);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		}
    }

}
