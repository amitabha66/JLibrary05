package org.rzo.netty.ahessian.example.chat.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.rzo.netty.ahessian.example.chat.service.ContinuationChatServiceImpl;

public class ChatServer
{
    public static void main(String[] args)
    {
        Executor executor = Executors.newFixedThreadPool(200);

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new OioServerSocketChannelFactory(
                		executor,
                		executor));

        // create just one service for all connections
        ContinuationChatServiceImpl service = new ContinuationChatServiceImpl();
        bootstrap.setPipelineFactory(
               new ChatServerSessionPipelineFactory( new ChatServerMixinPipelineFactory(executor, service)));

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(8080));

    }


}
