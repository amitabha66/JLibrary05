package org.rzo.netty.ahessian.acl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * ACLFilter is an Access Control List filter. In a server it allows to specify
 * which computers (ip or name) are permitted to access the server. The filter
 * checks for allowed AND NOT blocked <br>
 * An ACL is a string with the following syntax: <br>
 * 
 * <pre>
 * acl ::= (allow | block)[,(allow | block)]*
 * allow ::= +computer                
 * block ::= -computer
 * computer ::= [n|i]:address          n stands for computer name, i for ip address
 * address ::= &lt;regex&gt; | localhost
 * regex is a regular expression with '*' as multi character and '?' as single character wild card
 * </pre>
 * 
 * <br>
 * 
 * Example: allow only localhost:
 * 
 * +n:localhost,-n:*
 * 
 * Example: allow only local lan:
 * 
 * +i:192.168.0.*,-n:*
 * 
 * <br>
 * A typical setup for ACL for TCP/IP socket would be:
 * 
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
 * 
 * ACLFilter acl = new ACLFilter();
 * acl.setACL(&quot;+n:localhost,-n:*&quot;);
 * pipeline.addFirst(&quot;firewall&quot;, acl);
 * </pre>
 * 
 */
@ChannelPipelineCoverage("one")
public class ACLFilter extends SimpleChannelUpstreamHandler
{
	Pattern	_allowIpPattern		= null;
	Pattern	_blockIpPattern		= null;
	Pattern	_allowNamePattern	= null;
	Pattern	_blockNamePattern	= null;
	boolean	_allowLocalhost		= false;
	boolean	_blockLocalhost		= false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.
	 * jboss.netty.channel.ChannelHandlerContext,
	 * org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
	{
		boolean blocked = true;
		try
		{
			blocked = isAllowed(ctx.getChannel()) || !isBlocked(ctx.getChannel());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		if (blocked)
		{
			// forward if allowed or not blocked
			ctx.sendUpstream(e);
		}
		else
		{
			blockSession(ctx.getChannel());
		}
	}

	/**
	 * Sets the aCL.
	 * 
	 * @param acl
	 *            the new aCL
	 */
	public void setACL(String acl)
	{
		resetACL();
		parseACL(acl);
	}

	/**
	 * Reset acl.
	 */
	public void resetACL()
	{
		_allowIpPattern = null;
		_blockIpPattern = null;
		_allowNamePattern = null;
		_blockNamePattern = null;
	}

	/**
	 * Checks if is blocked.
	 * 
	 * @param session
	 *            the session
	 * 
	 * @return true, if is blocked
	 * @throws UnknownHostException
	 */
	private boolean isBlocked(Channel session) throws UnknownHostException
	{
		InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
		if (_blockLocalhost)
			if (isLocalhost(remoteAddress.getAddress()))
				return true;
		if (_blockIpPattern != null)
			if (_blockIpPattern.matcher(remoteAddress.getAddress().getHostAddress()).matches())
				return true;
		if (_blockNamePattern != null)
			if (_blockNamePattern.matcher(remoteAddress.getHostName()).matches())
				return true;

		return false;
	}

	/**
	 * Checks if is allowed.
	 * 
	 * @param session
	 *            the session
	 * 
	 * @return true, if is allowed
	 * @throws UnknownHostException
	 */
	private boolean isAllowed(Channel session) throws UnknownHostException
	{
		InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
		if (_allowLocalhost)
			if (isLocalhost(remoteAddress.getAddress()))
				return true;
		if (_allowIpPattern != null)
			if (_allowIpPattern.matcher(remoteAddress.getAddress().getHostAddress()).matches())
				return true;
		if (_allowNamePattern != null)
			if (_allowNamePattern.matcher(remoteAddress.getHostName()).matches())
				return true;

		return false;
	}

	private boolean isLocalhost(InetAddress address)
	{
		try
		{
			if (address.equals(InetAddress.getLocalHost()))
				return true;
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		try
		{
			InetAddress[] addrs = InetAddress.getAllByName("127.0.0.1");
			for (InetAddress addr : addrs)
				if (addr.equals(address))
					return true;
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		return false;

	}

	/**
	 * Parses the acl.
	 * 
	 * @param acl
	 *            the acl
	 */
	private void parseACL(String acl)
	{
		String allowI = "";
		String allowN = "";
		String blockI = "";
		String blockN = "";

		String[] acls = acl.split(",");
		for (String c : acls)
		{
			c = c.trim();
			if (c.equals("+n:localhost"))
				_allowLocalhost = true;
			else if (c.equals("-n:localhost"))
				_blockLocalhost = true;
			else if (c.startsWith("+n:"))
				allowN = addRule(allowN, c.substring(3));
			else if (c.startsWith("+i:"))
				allowI = addRule(allowI, c.substring(3));
			else if (c.startsWith("-n:"))
				blockN = addRule(blockN, c.substring(3));
			else if (c.startsWith("-i:"))
				blockI = addRule(blockI, c.substring(3));
		}
		if (allowI != null && allowI.length() != 0)
			_allowIpPattern = Pattern.compile(allowI);
		if (allowN != null && allowN.length() != 0)
			_allowNamePattern = Pattern.compile(allowN);
		if (blockI != null && blockI.length() != 0)
			_blockIpPattern = Pattern.compile(blockI);
		if (blockN != null && blockN.length() != 0)
			_blockNamePattern = Pattern.compile(blockN);

	}

	private void setPattern(Pattern pattern, String rule)
	{
		if (rule == null || rule.length() == 0)
			return;
		pattern = Pattern.compile(rule);
	}

	private String addRule(String pattern, String rule)
	{
		if (rule == null || rule.length() == 0)
			return pattern;
		if (pattern.length() != 0)
			pattern += "|";
		rule = rule.replaceAll("\\.", "\\\\.");
		rule = rule.replaceAll("\\*", ".*");
		rule = rule.replaceAll("\\?", ".");
		pattern += "(" + rule + ")";
		return pattern;
	}

	/**
	 * Block session.
	 * 
	 * @param channel
	 *            the channel
	 */
	private void blockSession(Channel channel)
	{
		System.out.println("connection refused : " + channel.getRemoteAddress());
		channel.close();
	}

}
