package org.rzo.netty.ahessian.example.chat.client;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.rzo.netty.ahessian.auth.ClientAuthFilter;
import org.rzo.netty.ahessian.auth.EncryptedAuthToken;
import org.rzo.netty.ahessian.example.log.OutLogger;
import org.rzo.netty.ahessian.session.ClientSessionFilter;

public class ChatClientSessionPipelineFactory implements ChannelPipelineFactory
{

	ChannelPipelineFactory _mixinFactory;
	ClientSessionFilter _sessionFilter;
	
	public ChatClientSessionPipelineFactory(ChannelPipelineFactory mixinFactory)
	{
		_mixinFactory = mixinFactory;
		_sessionFilter = new ClientSessionFilter(_mixinFactory);
	}
	
	public ChannelPipeline getPipeline() throws Exception
	{	
    ChannelPipeline pipeline = pipeline(); // Note the static import.
    pipeline.addLast("logger",new OutLogger("1"));
    
//    // authentification example
//    EncryptedAuthToken token = new EncryptedAuthToken();
//    token.setAlgorithm("SHA-1");
//    token.setPassword("test");
//    ClientAuthFilter auth = new ClientAuthFilter(token);
//    pipeline.addLast("auth", auth);

    pipeline.addLast("sessionFilter", _sessionFilter);
    return pipeline;
	}

}
