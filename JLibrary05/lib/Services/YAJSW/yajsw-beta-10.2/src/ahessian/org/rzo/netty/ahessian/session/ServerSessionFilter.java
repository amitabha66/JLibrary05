package org.rzo.netty.ahessian.session;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

/**
 * Handles sessions on the server side. A typical setup for a
 * protocol in a TCP/IP socket would be:
 * 
 * <pre>
 * // _mixinFactory is a ChannelPipelineFactory which returns MixinPipeline
 * {@link ChannelPipeline} pipeline = ...;
 * 
 * pipeline.addLast(&quot;sessionFilter&quot;, new ServerSessionFilter(_mixinFactory));
 * </pre>
 */
@ChannelPipelineCoverage("one")
public class ServerSessionFilter extends SimpleChannelUpstreamHandler
{
	
	/** Indicates if session has been assigned to the current channel */
	private boolean								_hasSession			= false;
	
	/** String for reading in a session id */
	private String								_sessionId			= "";
	
	/** Factory for creating new session objects */
	private SessionFactory						_factory			= new SessionFactory();
	
	/** Connected event is intercepted. It is sent upstream only after a session has been established*/
	private ChannelStateEvent					_connectedEvent;
	
	/** A pipeline factory which returns a MixinPipeline */
	private ChannelPipelineFactory				_mixinFactory;
	
	/** Assignment of session-id to the associated MixinPipeline */
	private static Map<String, MixinPipeline>	_sessionPipelines	= Collections.synchronizedMap(new HashMap<String, MixinPipeline>());

	/**
	 * Instantiates a new server session filter.
	 * 
	 * @param mixinFactory a pipeline factory which returns MixinPipeline
	 */
	public ServerSessionFilter(ChannelPipelineFactory mixinFactory)
	{
		_mixinFactory = mixinFactory;
	}

	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
	{
		// if session established forward all messages
		if (_hasSession)
			ctx.sendUpstream(e);
		else
		{
			ChannelBuffer b = (ChannelBuffer) e.getMessage();
			_sessionId += b.toString("UTF-8");
			if (_sessionId.equals("?"))
				newSession(ctx);
			else
				checkSession(ctx);
		}
	}

	private void checkSession(ChannelHandlerContext ctx)
	{
		if (_sessionId.length() == _factory.getSessionIdLength() * 2)
		{
			Session session = _factory.getSession(_sessionId);
			if (session == null)
				newSession(ctx);
			else
				confirmSession(ctx);
		}

	}

	private void newSession(ChannelHandlerContext ctx)
	{
		Session session = _factory.createSession(null);
		MixinPipeline pipeline = null;
		try
		{
			pipeline = (MixinPipeline) _mixinFactory.getPipeline();
			_sessionPipelines.put(session.getId(), pipeline);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		handleSession(ctx, session, pipeline);
	}

	private void confirmSession(ChannelHandlerContext ctx)
	{
		Session session = _factory.getSession(_sessionId);
		MixinPipeline pipeline = _sessionPipelines.get(_sessionId);
		handleSession(ctx, session, pipeline);
	}

	private void handleSession(ChannelHandlerContext ctx, Session session, MixinPipeline pipeline)
	{
		_hasSession = true;
		// now that we have a session extend the pipeline
		ChannelPipeline currentPipeline = ctx.getPipeline();
		pipeline.mixin(currentPipeline);
		ctx.setAttachment(session);
		ctx.sendUpstream(_connectedEvent);
		// send the session id to client
		ChannelFuture future = Channels.future(ctx.getChannel());
		Channels.write(ctx, future, ChannelBuffers.wrappedBuffer(session.getId().getBytes()));
		// flush
		Channels.write(ctx, future, ChannelBuffers.EMPTY_BUFFER);
	}

	/**
	 * Helper Method: returns the session of associated with the pipeline of a given context
	 * 
	 * @param ctx the context
	 * 
	 * @return the session
	 */
	public static Session getSession(ChannelHandlerContext ctx)
	{
		ChannelHandlerContext handler = ctx.getPipeline().getContext(ServerSessionFilter.class);
		if (handler == null)
			return null;
		return (Session) handler.getAttachment();
	}

	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		// remeber the event. it will be sent upstream when session has been
		// created
		_connectedEvent = e;
	}

	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		_hasSession = false;
		_sessionId = "";
		_connectedEvent = null;
	}
	
	/**
	 * Helper method: Gets the session from the pipeline of a given context.
	 * 
	 * @param ctx the context
	 * 
	 * @return the output stream
	 */
	public static Session getSEssion(ChannelHandlerContext ctx)
	{
		return (Session) ctx.getPipeline().getContext(ServerSessionFilter.class).getAttachment();
	}


}
