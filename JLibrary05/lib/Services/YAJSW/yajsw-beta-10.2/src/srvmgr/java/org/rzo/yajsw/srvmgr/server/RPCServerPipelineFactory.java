package org.rzo.yajsw.srvmgr.server;

import static org.jboss.netty.channel.Channels.pipeline;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallDecoder;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyEncoder;
import org.rzo.netty.ahessian.rpc.server.HessianRPCServiceHandler;
import org.rzo.netty.ahessian.acl.ACLFilter;
import org.rzo.netty.ahessian.auth.EncryptedAuthToken;
import org.rzo.netty.ahessian.auth.ServerAuthFilter;
import org.rzo.netty.ahessian.io.InputStreamDecoder;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import org.rzo.netty.ahessian.rpc.server.HessianSkeleton;

public class RPCServerPipelineFactory implements ChannelPipelineFactory
{
	Executor _executor;
	HessianRPCServiceHandler _handler;
	String _acl = null;

	RPCServerPipelineFactory(Executor executor, HessianRPCServiceHandler handler, String acl)
	{
		_executor = executor;
		_handler = handler;
		_acl = acl;
	}
	
	public ChannelPipeline getPipeline() throws Exception
	{
	    ChannelPipeline pipeline = pipeline(); // Note the static import.
	    if (_acl != null)
	    {
	    	ACLFilter acl = new ACLFilter();
	    	acl.setACL(_acl);
	    	pipeline.addFirst("firewall", acl);
	    }
        //pipeline.addLast("logger1",new OutLogger("1"));
        pipeline.addLast("inputStream", new InputStreamDecoder(_executor));
        //pipeline.addLast("logger2",new OutLogger("2"));
        pipeline.addLast("outputStream", new OutputStreamEncoder());
        //pipeline.addLast("logger3",new OutLogger("3"));
        pipeline.addLast("callDecoder", new HessianRPCCallDecoder());
        pipeline.addLast("replyEncoder", new HessianRPCReplyEncoder());
        //pipeline.addLast("logger4",new OutLogger("4"));
        pipeline.addLast("hessianRPCServer", _handler);
        
        //bootstrap.getPipeline().addLast("logger4",new OutLogger("4"));
        return pipeline;
	}

}
