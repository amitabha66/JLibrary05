package org.rzo.netty.ahessian.io;

import java.io.OutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Encodes bytes written to an {@link OutputStream} into a {@link ChannelBuffer}
 * . A typical setup for a serialization protocol in a TCP/IP socket would be:
 * 
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
 * 
 * // Encoder
 * pipeline.addLast(&quot;outputStream&quot;, new {@link handler.io.OutputStream}());
 * pipeline.addLast(&quot;outputHandler&quot;, new MyOutputHandler());
 * 
 * // Decoder
 * pipeline.addLast(&quot;inputStream&quot;, new {@link handler.io.InputStream}());
 * pipeline.addLast(&quot;inputHandler&quot;, new MyInputHandler());
 * </pre>
 * 
 * and then, within the handler you can use a {@link java.io.InputStream} or
 * {@link java.io.OutputStream} instead of a {@link ChannelBuffer} as a message: <br>
 * Writing to OutputStream:
 * 
 * <pre>
 * // synchronized for multithreaded environment to avoid messages mixing
 * synchronized public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception
 * {
 * byte[] message = (byte[]) e.getMessage();
 * OutputStream out = OutputStreamEncoder.getOutputStream(ctx);
 * out.write(message);
 * // if this is the last chunk of bytes we should flush the output
 * out.flush();
 * // netty seems to require this, so that the boss thread may read input from the channel
 * Thread.yield();
 * }
 * 
 * </pre>
 * 
 * <br>
 * Reading from InputStream:
 * 
 * <pre>
 * void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
 * {
 * // message received is called only once to deliver the input stream
 * // it is called in a separate thread and not in the netty worker thread.
 * // incoming bytes are consumed in this method.
 * // the stream is closed once the channel is disconnected
 * InputStream in = (InputStream) evt.getMessage();
 * 
 * while (ctx.getChannel().isConnected())
 * {
 * // parse the incoming stream and forward the result to the next handler
 * Channels.fireMessageReceived(ctx, parseReply(in));
 * }
 * }
 * </pre>
 */
@ChannelPipelineCoverage("all")
public class OutputStreamEncoder extends SimpleChannelHandler
{
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		ctx.setAttachment(new OutputStreamBuffer(ctx));
		ctx.sendUpstream(e);
	}

	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		OutputStreamBuffer out = (OutputStreamBuffer) ctx.getAttachment();
		if (out != null)
		{
			out.close();
		}
		ctx.sendUpstream(e);
	}

	/**
	 * Helper method: Gets the output stream from the pipeline of a given context.
	 * 
	 * @param ctx the context
	 * 
	 * @return the output stream
	 */
	public static OutputStream getOutputStream(ChannelHandlerContext ctx)
	{
		return (OutputStream) ctx.getPipeline().getContext(OutputStreamEncoder.class).getAttachment();
	}

}
