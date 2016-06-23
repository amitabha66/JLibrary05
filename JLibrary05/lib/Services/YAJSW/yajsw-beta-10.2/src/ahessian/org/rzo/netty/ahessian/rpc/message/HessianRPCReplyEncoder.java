package org.rzo.netty.ahessian.rpc.message;

import org.rzo.netty.ahessian.rpc.io.Hessian2Output;
import org.rzo.netty.ahessian.rpc.io.HessianOutput;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

import java.io.OutputStream;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

/**
 * writes an invocation reply message to an output stream
 */
@ChannelPipelineCoverage("one")
public class HessianRPCReplyEncoder extends SimpleChannelDownstreamHandler
	{

		/* (non-Javadoc)
		 * @see org.jboss.netty.channel.SimpleChannelDownstreamHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
		 */
	Hessian2Output hOut = null;
		synchronized public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception
		{
			HessianRPCReplyMessage message = (HessianRPCReplyMessage) e.getMessage();
			OutputStream out = OutputStreamEncoder.getOutputStream(ctx);
			if (hOut == null)
				hOut = new Hessian2Output(out);
			hOut.writeReply(message);
			hOut.flush();
			hOut.resetReferences();
			//Thread.yield();
		}

}
