package org.rzo.netty.ahessian.example.chat.server;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.rzo.netty.ahessian.acl.ACLFilter;
import org.rzo.netty.ahessian.auth.EncryptedAuthToken;
import org.rzo.netty.ahessian.auth.ServerAuthFilter;
import org.rzo.netty.ahessian.example.log.OutLogger;
import org.rzo.netty.ahessian.session.ServerSessionFilter;

public class ChatServerSessionPipelineFactory implements ChannelPipelineFactory
{

	ChannelPipelineFactory _mixinFactory;
	
	public ChatServerSessionPipelineFactory(ChannelPipelineFactory mixinFactory)
	{
		_mixinFactory = mixinFactory;
	}
	
	public ChannelPipeline getPipeline() throws Exception
	{	
    ChannelPipeline pipeline = pipeline(); // Note the static import.
    pipeline.addLast("logger1",new OutLogger("1"));
    
//    // ACL example
//    ACLFilter acl = new ACLFilter();
//    acl.setACL("+i:172.*,+i:127.0.0.1,-n:*");
//    pipeline.addFirst("firewall", acl);
//    
//    // authentification example
//    EncryptedAuthToken token = new EncryptedAuthToken();
//    token.setAlgorithm("SHA-1");
//    token.setPassword("test");
//    ServerAuthFilter auth = new ServerAuthFilter(token);
//    pipeline.addLast("auth", auth);
    
    //pipeline.addLast("logger2",new OutLogger("2"));
    
    pipeline.addLast("sessionFilter", new ServerSessionFilter(_mixinFactory));
    return pipeline;
	}

}
