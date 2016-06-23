package org.rzo.netty.discovery;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observer;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class DiscoveryClient extends MulticastEndpoint
{

private String	name;
private Set<String>	hosts = Collections.synchronizedSet(new HashSet<String>());
private boolean stop = false;
private Set<DiscoveryListener>	listeners = Collections.synchronizedSet(new HashSet<DiscoveryListener>());


public void init() throws Exception
{
	ChannelPipelineFactory factory = new ChannelPipelineFactory()
	{
		public ChannelPipeline getPipeline() throws Exception
		{
			ChannelPipeline pipeline = pipeline();
			pipeline.addLast("discoveryServer", new SimpleChannelUpstreamHandler()
			{

				@Override
				public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
				{
					try
					{
					String response = getMessage(e);
					if (response == null)
						return;
					String[] resp = response.split(":");
					if (resp.length == 2)
					{
						String host = resp[0];
						InetAddress.getByName(host);
						int port = Integer.parseInt(resp[1]);
						if (!hosts.contains(response))
						{
						hosts.add(response);
						for (DiscoveryListener listener : listeners)
						{
							listener.newHost(name, response);
						}
						}
						
					}
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			});
			return pipeline;
		}

	};
	super.init(factory);
	discoverServices();
}



private void discoverServices() throws Exception
{
	executor.execute(new Runnable()
	{
		public void run()
		{
			while (!stop)
			{
				try
				{
					send(ChannelBuffers.wrappedBuffer((name).getBytes()));
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try
				{
					Thread.sleep(10000);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	});
}



public String getName()
{
	return name;
}


public void setName(String name)
{
	this.name = name;
}

public void stop()
{
	stop = true;
	super.close();
}

public void addListener(DiscoveryListener listener)
{
	listeners.add(listener);
}

public void removeHost(String host)
{
	hosts.remove(host);
}

}

