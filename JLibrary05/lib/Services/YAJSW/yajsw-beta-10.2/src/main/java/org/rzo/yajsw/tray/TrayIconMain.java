package org.rzo.yajsw.tray;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.rzo.yajsw.Constants;
import org.rzo.yajsw.boot.WrapperLoader;
import org.rzo.yajsw.cache.Cache;
import org.rzo.yajsw.config.YajswConfigurationImpl;
import org.rzo.yajsw.wrapper.AbstractWrappedProcessMBean;
import org.rzo.yajsw.wrapper.TrayIconProxy;
import org.rzo.yajsw.wrapper.TrayIconProxy.Types;

public class TrayIconMain
{
	 volatile static JMXConnector jmxc = null;
	 static JMXServiceURL url = null;
	 static String user = null;
	 static String password = null;
	 volatile static AbstractWrappedProcessMBean proxy = null;
	 static ObjectName oName = null;
	 static WrapperTrayIconImpl _trayIcon = null;
	 static FileLock lock = null;
	 
		private static String getName(Configuration _config)
		{
			String result = "";
			if (_config == null)
				return result;
			if (_config.getBoolean("wrapper.service", false))
				result += "Service ";
			String name = _config.getString("wrapper.console.title");
			if (name == null)
				name = _config.getString("wrapper.ntservice.name");
			if (name == null)
				name = _config.getString("wrapper.image");
			if (name == null)
				name = _config.getString("wrapper.groovy");
			if (name == null)
				name = _config.getString("wrapper.java.app.mainclass");
			if (name == null)
				name = _config.getString("wrapper.java.app.jar");
			if (name == null)
				name = "";
			result += name;
			return result;

		}


	 
	private static void reconnect() throws InterruptedException
	{
		_trayIcon.closeConsole();
		_trayIcon.setProcess(null);
		System.out.println("trying to connect");
		while (jmxc == null)
			try
			{
				if (_trayIcon.isStop())
					return;
				// TODO disableFunctions();
				
				Map environment = null;
				if (user != null)
				{
					environment = new HashMap();
					String[] credentials = new String[]{user, password};
					environment.put(JMXConnector.CREDENTIALS, credentials);
				}
				
				jmxc = JMXConnectorFactory.connect(url, environment);
				jmxc.addConnectionNotificationListener(new NotificationListener()
				{

					public void handleNotification(Notification notification, Object handback)
					{
						if (notification.getType().equals(JMXConnectionNotification.CLOSED) || notification.getType().equals(JMXConnectionNotification.FAILED))
						{
							try
							{
								try
								{
									jmxc.close();
								}
								catch (IOException e)
								{
									System.out.println(e.getMessage());
								}
								jmxc = null;
								reconnect();
							}
							catch (InterruptedException e)
							{
								System.out.println(e.getMessage());
								Thread.currentThread().interrupt();
							}
						}
					}
					
				},
				null, null);
		      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
				proxy = (AbstractWrappedProcessMBean)
		        MBeanServerInvocationHandler.newProxyInstance( 
		                                       mbsc, 
		                                       oName, 
		                                       AbstractWrappedProcessMBean.class, 
		                                       false);
				_trayIcon.setProcess(proxy);
				System.out.println("connected");
			}
			catch (IOException e)
			{
					Thread.sleep(1000);
			}
			
	}
	public static void main(String[] args) throws IOException
	{
		String config = null;
		if (args.length >0)
			config = args[0];
		File f = new File(config);
		if (!f.exists())
		{
			System.out.println("file not found "+f);
			config = null;
		}
		
		if (config != null)
		{
			String canonName = new File(config).getCanonicalPath();
			File lockFile = new File(System.getProperty("java.io.tmpdir"), ""+"yajsw."+canonName.hashCode()+".lck");
			System.out.println("system tray lock file: " + lockFile.getCanonicalPath());
			FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
	        // Try acquiring the lock without blocking. This method returns
	        // null or throws an exception if the file is already locked.
	        try {
	            lock = channel.tryLock();
	        } catch (OverlappingFileLockException e) {
	            // File is already locked in this thread or virtual machine
	        	return;	
	        }
	     if (lock == null)
	    	 return;
		}
		System.setProperty("wrapper.config", config);
			
		Configuration localConf = new BaseConfiguration();
		if (config != null)
			localConf.addProperty("wrapper.config", config);
		YajswConfigurationImpl _config = new YajswConfigurationImpl(localConf, true);
		  int port = _config.getInt("wrapper.jmx.rmi.port", Constants.DEFAULT_RMI_PORT);
		  if (port > 0)
		  try {
		      url = new JMXServiceURL( 
		         "service:jmx:rmi:///jndi/rmi://localhost:"+port+"/server");
		      user = _config.getString("wrapper.jmx.rmi.user", null);
		      password = _config.getString("wrapper.jmx.rmi.password", "");
				String name = _config.getString("wrapper.console.title");
				if (name == null)
					name = _config.getString("wrapper.ntservice.name");
				if (name == null)
					name = "yajsw.noname";
				 oName = new ObjectName("org.rzo.yajsw", "name", name);
				 _trayIcon = (WrapperTrayIconImpl) WrapperTrayIconFactory.createTrayIcon(getName(_config), _config.getString("wrapper.tray.icon"));
				 reconnect();
								while (!_trayIcon.isStop())
								{
									if (jmxc != null && proxy != null)
									try
									{
									if (!showInquire(proxy.getInquireMessage()))
									{
										showMessages(proxy.getTrayIconMessages());
										_trayIcon.showState(proxy.getState());
									}
									}
									catch (Exception ex)
									{
										System.out.println(ex.getMessage());
										jmxc = null;
										reconnect();
									}
									//System.out.println(">> "+proxy);
									try
									{
										Thread.sleep(1000);
									}
									catch (InterruptedException e)
									{
										System.out.println(e.getMessage());
										Thread.currentThread().interrupt();
									}
								}
		  }
		  catch (Exception ex)
		  {
				System.out.println(ex.getMessage());
			  return;
		  }
		  else
		  {
			  System.out.println("no jmx rmi port defined -> abort");
			  return;
		  }


	}



	private static boolean showInquire(String message)
	{
		if (message == null)
			return false;
		if (_trayIcon == null)
			return false;
		if (_trayIcon._inquireMessage == null)
		{
			_trayIcon.message("Input Required", message + "\n enter data through response menue");
			_trayIcon._inquireMessage = message;
			return true;
		}
		return true;

	}



	private static void showMessages(String[][] messages)
	{
		if (_trayIcon == null)
			return;
		if (messages == null)
			return;
		for (String[] message : messages)
		{
			Types type = Types.valueOf(message[0]);
			switch (type)
			{
			case ERROR:
				_trayIcon.error(message[1], message[2]);
				break;
			case INFO:
				_trayIcon.info(message[1], message[2]);
				break;
			case MESSAGE:
				_trayIcon.message(message[1], message[2]);
				break;
			case WARNING:
				_trayIcon.warning(message[1], message[2]);
				break;
			default :
				System.out.println("wrong message type");
			}
		}
	}
	
	

}
