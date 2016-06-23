package org.rzo.netty.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;

public class MulticastEndpoint
{
	
    private String mcastGroupIp = "228.10.10.10";
    private int mcastGroupPort = 12345;
    private String bindAddress = "192.168.0.10";
    
    private DatagramChannel datagramChannel;
    private ConnectionlessBootstrap connectionlessBootstrap;
    private InetSocketAddress multicastAddress;
    Executor executor = Executors.newCachedThreadPool();
    String id;
    
	public void  init(ChannelPipelineFactory factory) throws Exception
	{
			id = String.format("%1$020d", Math.abs(new Random(System.currentTimeMillis()).nextLong()));
			DatagramChannelFactory datagramChannelFactory = new
	        OioDatagramChannelFactory(executor);

	         connectionlessBootstrap = new
	        ConnectionlessBootstrap(datagramChannelFactory);
	        connectionlessBootstrap.setOption("broadcast", true);
	        connectionlessBootstrap.setPipelineFactory(factory);
	        datagramChannel = (DatagramChannel)
	        connectionlessBootstrap.bind(new InetSocketAddress(mcastGroupPort));
	        multicastAddress = new InetSocketAddress(mcastGroupIp, mcastGroupPort);
	        NetworkInterface networkInterface =
	        NetworkInterface.getByInetAddress(InetAddress.getByName(bindAddress));
	        datagramChannel.joinGroup(multicastAddress, networkInterface);
	}
	
	public void send(ChannelBuffer msg) throws Exception
	{
		ChannelBuffer toWrite = ChannelBuffers.dynamicBuffer();
		toWrite.writeBytes(id.getBytes());
		toWrite.writeBytes(msg.array());
		datagramChannel.write(toWrite, multicastAddress);
	}

	public String getMcastGroupIp()
	{
		return mcastGroupIp;
	}

	public int getMcastGroupPort()
	{
		return mcastGroupPort;
	}

	public String getBindAddress()
	{
		return bindAddress;
	}

	public void setMcastGroupIp(String mcastGroupIp)
	{
		this.mcastGroupIp = mcastGroupIp;
	}

	public void setMcastGroupPort(int mcastGroupPort)
	{
		this.mcastGroupPort = mcastGroupPort;
	}

	public void setBindAddress(String bindAddress)
	{
		this.bindAddress = bindAddress;
	}
	
	public void close()
	{
		datagramChannel.close();
		connectionlessBootstrap.releaseExternalResources();
	}
	
	public String getMessage(MessageEvent e)
	{
		String response = ((ChannelBuffer) e.getMessage()).toString(Charset.defaultCharset());
		String msgId = response.substring(0,20);
		if (msgId.equals(id))
			return null;
		return response.substring(20);

	}
	

}
