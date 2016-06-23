package org.rzo.netty.ahessian.example.rpc.service;

import java.util.Arrays;

public class HelloWorldService implements HelloWorldServiceInterface
{
	static int count = 0;
	static char[] x = new char[1000];
	static {Arrays.fill(x, 'a');}
	static String result = new String(x);
	public String HelloWorld(String i) 
	{
		//System.out.println("got "+i);
		//System.out.println("hello world");
		System.out.println(Thread.currentThread());
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "hello world " +count++ ;//+ " "+i+result;
	}

}
