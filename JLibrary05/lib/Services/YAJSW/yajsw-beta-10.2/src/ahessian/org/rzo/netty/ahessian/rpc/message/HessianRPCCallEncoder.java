package org.rzo.netty.ahessian.rpc.message;

import org.rzo.netty.ahessian.rpc.io.Hessian2Output;
import org.rzo.netty.ahessian.rpc.io.HessianOutput;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

import java.io.OutputStream;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.caucho.hessian.io.SerializerFactory;

/**
 * writes a call request to an output stream
 */
@ChannelPipelineCoverage("all")
public class HessianRPCCallEncoder extends SimpleChannelDownstreamHandler
{
	SerializerFactory sFactory = new SerializerFactory();
	Hessian2Output hOut = null;

	 /* (non-Javadoc)
 	 * @see org.jboss.netty.channel.SimpleChannelDownstreamHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
 	 */
 	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception
	{
		HessianRPCCallMessage message = (HessianRPCCallMessage) e.getMessage();
		//System.out.println("encode "+message.getHeaders().get("call-id"));
		OutputStream out = (OutputStream) ctx.getPipeline().getContext(OutputStreamEncoder.class).getAttachment();
		if (hOut == null)
		{
			hOut = new Hessian2Output(out);
	    	hOut.setSerializerFactory(sFactory);
		}
		hOut.call(message);
		hOut.flush();
		//--Thread.yield();
	}

	private OutputStream getOutputStream(ChannelHandlerContext ctx)
	{
		return (OutputStream) ctx.getPipeline().getContext(OutputStreamEncoder.class).getAttachment();
	}

}