package org.rzo.netty.ahessian.example.rpc.service;

import org.rzo.netty.ahessian.rpc.server.Continuation;

public class ContinuationHalloWorldService extends HelloWorldService
{
	public void HelloWorld(Continuation continuation, String txt)
	{
//		for (int i=0; i<10; i++)
//		{
//			continuation.send(txt + " "+i);
//			try
//			{
//				Thread.sleep(1000);
//			}
//			catch (InterruptedException e)
//			{
//				e.printStackTrace();
//			}
//		}
		continuation.complete(super.HelloWorld(txt));
	}

}
