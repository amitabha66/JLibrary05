package org.rzo.netty.ahessian.example.rpc.client;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.rzo.netty.ahessian.session.ClientSessionFilter;

public class RPCClientSessionPipelineFactory implements ChannelPipelineFactory
{

	ChannelPipelineFactory _mixinFactory;
	ClientSessionFilter _sessionFilter;
	
	RPCClientSessionPipelineFactory(ChannelPipelineFactory mixinFactory)
	{
		_mixinFactory = mixinFactory;
		_sessionFilter = new ClientSessionFilter(_mixinFactory);
	}
	
	public ChannelPipeline getPipeline() throws Exception
	{	
    ChannelPipeline pipeline = pipeline(); // Note the static import.
    //pipeline.addLast("logger",new OutLogger("1"));
    pipeline.addLast("sessionFilter", _sessionFilter);
    return pipeline;
	}

}
