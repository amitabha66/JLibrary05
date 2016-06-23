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

/**
 * Handles sessions on the client side.
 * A typical setup for a protocol in a TCP/IP socket would be:
 * 
 * <pre>
 * // client session filter is an attribute of the ChannelPipelineFactory Class
 * // it should not be created with each call to getPipeline()
 * // _mixinFactory is a ChannelPipelineFactory which returns MixinPipeline
 * _sessionFilter = new ClientSessionFilter(_mixinFactory);
 * 
 * {@link ChannelPipeline} pipeline = ...;
 * 
 * pipeline.addLast("sessionFilter", _sessionFilter);
 * </pre>
 */

@ChannelPipelineCoverage("all")
public class ClientSessionFilter extends SimpleChannelUpstreamHandler
{
	
	/** The current session. */
	private Session _session = null;
	
	/** Indicates if we have received a session. */
	private boolean _hasSession = false;
	
	/** String to read in the session id from the server */
	private String _sessionId = "";
	
	/** Factory for creating session objects. */
	private SessionFactory _factory = new SessionFactory();
	
	/** Connected events are intercepted and sent upstream once a session has been established. */
	private ChannelStateEvent _connectedEvent;	
	
	/** The factory for getting a MixinPipeline for a new session */
	private ChannelPipelineFactory _mixinFactory;
	
	/** Assignment of session-id to pipelines created. //TODO destroy a pipeline if a session is timed out */
	private static Map<String, MixinPipeline> _sessionPipelines = Collections.synchronizedMap(new HashMap<String, MixinPipeline>());
	
	/**
	 * Instantiates a new client session filter.
	 * 
	 * @param mixinFactory the mixin factory
	 */
	public ClientSessionFilter(ChannelPipelineFactory mixinFactory)
	{
		_mixinFactory = mixinFactory;
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	// remeber the event. it will be sent upstream when session has been created
		_connectedEvent = e;
		String id = _session == null ? "?" : _session.getId();
		// send the session id to client
		ChannelFuture future = Channels.future(ctx.getChannel());
		Channels.write(ctx, future, ChannelBuffers.wrappedBuffer(id.getBytes()));
		// flush
		Channels.write(ctx, future, ChannelBuffers.EMPTY_BUFFER);
    }
	
    /* (non-Javadoc)
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) throws Exception 
            {
    	// if session established forward all messages
    			if (_hasSession)
    				ctx.sendUpstream(e);
    			else
    			{
    				ChannelBuffer b = (ChannelBuffer) e.getMessage();
    				_sessionId += b.toString("UTF-8");
    				checkSession(ctx);
    			}
            }
    
	private void checkSession(ChannelHandlerContext ctx)
	{
		if (_sessionId.length() == _factory.getSessionIdLength()*2)
		{
			if (_session == null)
				newSession(ctx);
			else if (_session.getId().equals(_sessionId))
				confirmSession(ctx);
			else
				changedSession(ctx);
		}
			
	}

	private void changedSession(ChannelHandlerContext ctx)
	{
		closeSession(_session);
		newSession(ctx);
	}

	private void closeSession(Session session)
	{
		// TODO Auto-generated method stub
		
	}

	private void confirmSession(ChannelHandlerContext ctx)
	{
		MixinPipeline pipeline = _sessionPipelines.get(_session.getId());
		handleSession(ctx, pipeline);
	}

	private void newSession(ChannelHandlerContext ctx)
	{
		_session = _factory.createSession(_sessionId);
		MixinPipeline pipeline = null;
		try
		{
			pipeline = (MixinPipeline) _mixinFactory.getPipeline();
			_sessionPipelines.put(_session.getId(), pipeline);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		handleSession(ctx, pipeline);
	}
	
	private void handleSession(ChannelHandlerContext ctx, MixinPipeline pipeline)
	{
		_hasSession = true;
		// now that we have a session extend the pipeline
		ChannelPipeline currentPipeline = ctx.getPipeline();
		pipeline.mixin(currentPipeline);
		ctx.setAttachment(_session);
		ctx.sendUpstream(_connectedEvent);
	}


}
