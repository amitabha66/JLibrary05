package org.rzo.netty.discovery;

public class ClientTest
{
	public static void main(String[] args) throws Exception
	{
		DiscoveryClient discovery = new DiscoveryClient();
		discovery.setName("serviceManagerServer");
		discovery.addListener(new DiscoveryListener()
		{

			public void newHost(String name, String host)
			{
				System.out.println(name + " " + host);
			}
			
		});
		discovery.init();
		
	}

}
