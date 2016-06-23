package org.rzo.netty.ahessian.example.log;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.logging.LoggingHandler;

public class OutLogger extends LoggingHandler
{
	String _name;
	public OutLogger(String name)
	{
		super(name);
		_name = name;
	}
    @Override
	public void log(ChannelEvent e)
	{
		//System.out.println(e);
		if (e instanceof DownstreamMessageEvent)
		{
			DownstreamMessageEvent devm = (DownstreamMessageEvent) e;
			Object mes = devm.getMessage();
			println(e.getChannel().getId()+" >>>");
			if (mes instanceof ChannelBuffer)
			System.out.println(((ChannelBuffer)((DownstreamMessageEvent)e).getMessage()).toString("UTF-8"));
			else
				System.out.println(mes);
		} else
		if (e instanceof UpstreamMessageEvent)
		{
			println(e.getChannel().getId()+" <<<");
			Object message = ((UpstreamMessageEvent)e).getMessage();
			if (message instanceof ChannelBuffer)
			println(((ChannelBuffer)((UpstreamMessageEvent)e).getMessage()).toString("UTF-8"));
			else
				println(message);
		}
		else
			println(e);
	}
    
    private void println(Object x)
    {
    	System.out.println("["+_name+"] "+ x);
    }

}
