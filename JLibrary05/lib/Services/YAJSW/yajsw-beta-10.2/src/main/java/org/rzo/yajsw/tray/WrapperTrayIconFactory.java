package org.rzo.yajsw.tray;

import java.io.IOException;

import org.rzo.yajsw.boot.WrapperLoader;
import org.rzo.yajsw.config.YajswConfigurationImpl;
import org.rzo.yajsw.os.OperatingSystem;
import org.rzo.yajsw.util.File;
import org.rzo.yajsw.wrapper.AbstractWrappedProcessMBean;
import org.rzo.yajsw.wrapper.WrappedProcess;

import org.rzo.yajsw.os.Process;

public class WrapperTrayIconFactory
{
	public static WrapperTrayIcon createTrayIcon(String name, String icon)
	{
		WrapperTrayIcon result = null;
		try
		{
			result = new WrapperTrayIconImpl(name, icon);
		}
		catch (Throwable ex)
		{
			System.out.println("java version does not support SystemTray: " + ex.getMessage());
			ex.printStackTrace();
		}
		if (result == null || !result.isInit())
			result = new WrapperTrayIconDummy();
		return result;
	}

	public static Process startTrayIcon(YajswConfigurationImpl config)
	{
		if (config == null)
			return null;
		String wrapperConfFileName = config.getCachedPath(false);

		final Process _osProcess = OperatingSystem.instance().processManagerInstance().createProcess();
		
		try
		{
			_osProcess.setCommand(new String[]{getJava(), "-cp", WrapperLoader.getWrapperHome()+"/wrapper.jar", TrayIconMainBooter.class.getName(), wrapperConfFileName});
			_osProcess.setPipeStreams(false, false);
			_osProcess.setVisible(false);
			_osProcess.start();
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				public void run()
				{
					if (_osProcess != null)
						_osProcess.kill(0);
				}
			});
			return _osProcess;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getJava()
	{
		String result = System.getenv("java_exe");
		if (result == null)
		{
			result = System.getProperty("sun.boot.library.path");
			if (result != null)
				result = result+"/javaw";
			else
				result = "java";
		}
		return result;
	}

}
