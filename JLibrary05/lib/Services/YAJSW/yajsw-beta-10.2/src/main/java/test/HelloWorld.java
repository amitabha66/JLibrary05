package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;


public class HelloWorld
{
	static class MyWriter implements Runnable
	{
		public void run()
		{
			int i = 0;
			while (i<10)
			{
				System.out.println(i++);
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
	}
	
	// test for application main.
	public static void main(String[] args) throws Exception
	{
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		System.out.println(factory.getClass());
//		try
//		{
//			Configuration config = new BaseConfiguration();
//		}
//		catch (Throwable ex)
//		{
//			System.out.println("all ok we cannot access commons configuration");
//			ex.printStackTrace();
//		}
		System.out.println("args:");
		for (int i=0; i<args.length; i++)
			System.out.println(args[i]);
		final Vector v = new Vector();
		new File("test.txt").delete();
		final FileWriter fw = new FileWriter("test.txt");
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					int i=0;
					while (true)
					{
						i++;
						String line = in.readLine();
						// int c = System.in.read();
						// System.out.println(">"+c);
						System.out.println(">" + line);
						if (line == null)
						// if (c == -1)
						{

							Thread.sleep(1000);
						}
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				System.out.println("terminated");
			}
		}).start();

		Runtime.getRuntime().addShutdownHook(new Thread()
		{

			public void run()
			{
				System.out.println("Exception");

				int i=1;
				//while (i>0)
				//	System.out.println("asdfasd");
				//Runtime.getRuntime().halt(0);
				System.out.println("You wanna quit, hey?");
				try
				{
					fw.close();
					//Thread.sleep(15000);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// while(true);
			}

		});

		ArrayList list = new ArrayList();

		// System.out.println(Scheduler.class.getClassLoader());
		// System.out.println(Configuration.class.getClassLoader());
		// System.out.flush();
		int i = 0;
		// org.rzo.yajsw.WrapperMain.WRAPPER_MANAGER.threadDump();
		try
		{
			// Thread.sleep(10000);
		}
		catch (Exception e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		new Thread(new MyWriter()).start();
		new Thread(new MyWriter()).start();
		new Thread(new MyWriter()).start();
		// System.out.println(new BufferedReader(new
		// InputStreamReader(System.in)).readLine());
		//for (; i < 10;)
		while (true)
		{
			i++;
			System.out.println("a" + i);
			System.out.flush();
			// simulate jvm crash
			// while (i>3)
			// list.add("asfdasffsadfdsdfsaadfsasdasf");

			 //if (i ==20)
			 //org.rzo.yajsw.app.WrapperJVMMain.WRAPPER_MANAGER.restart();

			if (fw != null)
			try
			{
				//v.add(new byte[1000]);
				//fw.write("" + i + "\n");
				//fw.flush();
			}
			catch (Throwable e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(0);
			}
			if (i % 2 == 0)
				try
				{
					// WrapperJVMMain.WRAPPER_MANAGER.stop();
					Thread.sleep(500);
					//System.out.println("Exception");
					//System.out.flush();
					 //Runtime.getRuntime().halt(0);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		

		/*
		 * WrapperManager.instance.restart(); try { Thread.sleep(10000); } catch
		 * (InterruptedException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
		//System.exit(0);
		//System.out.println("hello world. short test");

	}

}
