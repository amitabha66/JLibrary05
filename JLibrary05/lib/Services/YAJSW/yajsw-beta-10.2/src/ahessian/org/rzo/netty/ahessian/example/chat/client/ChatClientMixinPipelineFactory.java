package org.rzo.netty.ahessian.example.chat.client;

import org.rzo.netty.ahessian.rpc.client.HessianProxyFactory;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallEncoder;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyDecoder;
import org.rzo.netty.ahessian.io.InputStreamDecoder;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;
import org.rzo.netty.ahessian.session.MixinPipeline;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

public class ChatClientMixinPipelineFactory implements ChannelPipelineFactory
{
	
	Executor _executor;
	HessianProxyFactory _factory;

	public ChatClientMixinPipelineFactory(Executor executor, HessianProxyFactory factory)
	{
		_executor = executor;
		_factory = factory;
	}
	

	
	public ChannelPipeline getPipeline() throws Exception
	{
        ChannelPipeline pipeline = new MixinPipeline();
        // InputStreamDecoder returns an input stream and calls the next handler in a separate thread

        pipeline.addLast("inputStream", new InputStreamDecoder(_executor));

        //pipeline.addLast("logger2",new OutLogger("2"));
        pipeline.addLast("outputStream", new OutputStreamEncoder());
        
        pipeline.addLast("hessianReplyDecoder", new HessianRPCReplyDecoder(_factory));
        pipeline.addLast("hessianCallEncoder", new HessianRPCCallEncoder());
        //pipeline.addLast("logger3",new OutLogger("3"));
        pipeline.addLast("hessianHandler", _factory);
        
        return pipeline;

	}


}
