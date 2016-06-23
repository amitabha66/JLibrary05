package org.rzo.netty.ahessian.example.rpc.server;

import org.rzo.netty.ahessian.example.rpc.service.ContinuationHalloWorldService;
import org.rzo.netty.ahessian.example.rpc.service.HelloWorldService;
import org.rzo.netty.ahessian.example.rpc.service.HelloWorldServiceInterface;
import org.rzo.netty.ahessian.rpc.message.HessianRPCCallDecoder;
import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyEncoder;
import org.rzo.netty.ahessian.rpc.server.ContinuationService;
import org.rzo.netty.ahessian.rpc.server.HessianRPCServiceHandler;
import org.rzo.netty.ahessian.rpc.server.ImmediateInvokeService;
import org.rzo.netty.ahessian.io.InputStreamDecoder;
import org.rzo.netty.ahessian.io.OutputStreamEncoder;
import org.rzo.netty.ahessian.session.MixinPipeline;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

public class RPCServerMixinPipelineFactory implements ChannelPipelineFactory
{
	
	Executor _executor;

	RPCServerMixinPipelineFactory(Executor executor)
	{
		_executor = executor;
	}
	
	public ChannelPipeline getPipeline() throws Exception
	{
		ChannelPipeline pipeline = new MixinPipeline();
        pipeline.addLast("inputStream", new InputStreamDecoder(_executor));
        //pipeline.addLast("logger2",new OutLogger("2"));
        pipeline.addLast("outputStream", new OutputStreamEncoder());
        pipeline.addLast("callDecoder", new HessianRPCCallDecoder());
        pipeline.addLast("replyEncoder", new HessianRPCReplyEncoder());
        //pipeline.addLast("logger3",new OutLogger("3"));
        HessianRPCServiceHandler factory =  new HessianRPCServiceHandler(_executor);
        //factory.addService("default", new ContinuationService(new ContinuationHalloWorldService(), HelloWorldServiceInterface.class, factory, _executor));
        factory.addService("default", new ImmediateInvokeService(new HelloWorldService(), HelloWorldServiceInterface.class, factory));
        pipeline.addLast("hessianRPCServer", factory);
        
        //bootstrap.getPipeline().addLast("logger4",new OutLogger("4"));
        return pipeline;
	}

}
