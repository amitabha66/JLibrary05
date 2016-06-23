/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package org.rzo.yajsw.app;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jnacontrib.win32.Win32Service;

import org.rzo.yajsw.Constants;
import org.rzo.yajsw.boot.WrapperLoader;
import org.rzo.yajsw.config.YajswConfigurationImpl;
import org.rzo.yajsw.os.OperatingSystem;
import org.rzo.yajsw.util.DaemonThreadFactory;
import org.rzo.yajsw.wrapper.WrappedProcess;
import org.rzo.yajsw.wrapper.WrappedProcessFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class WrapperMainService.
 */
public class WrapperMainServiceWin extends Win32Service
{

	/** The w. */
	static WrappedProcess			w;

	/** The service. */
	static WrapperMainServiceWin	service;

	/**
	 * Instantiates a new wrapper main service.
	 */
	public WrapperMainServiceWin()
	{
	}

	// this is the wrapper for services
	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		String wrapperJar = WrapperLoader.getWrapperJar();
		String homeDir = new File(wrapperJar).getParent();
		OperatingSystem.instance().setWorkingDir(homeDir);
		//System.out.println("wrapper working dir" + homeDir);
		YajswConfigurationImpl _config = new YajswConfigurationImpl(false);
		service	= new WrapperMainServiceWin();
		service.setServiceName(_config.getString("wrapper.ntservice.name"));
		w = WrappedProcessFactory.createProcess(_config);
		w.setService(service);

		// start the application
		//w.setDebug(true);
		w.init();
		w.start();
		// init the service for signaling with services.exe. app will hang
		// here until service is stopped
		service.init();
		w.getWrapperLogger().info("Win service terminated correctly");
		Runtime.getRuntime().halt(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jnacontrib.win32.Win32Service#onStart()
	 */
	@Override
	public void onStart()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jnacontrib.win32.Win32Service#onStop()
	 */
	@Override
	public void onStop()
	{
		// when wrapper service is stopped: stop the application
		//System.out.println("+ on stop");
		// use wrapper.control
		try
		{
		w.getWrapperLogger().info("+ Win service stop");
		if (w.isHaltAppOnWrapper())
		{
			w.getWrapperLogger().info("+ Win service stop isHaltAppOnWrapper");
			// remove the listener, so it does not call System.exit
		w.removeStateChangeListener(WrappedProcess.STATE_IDLE);
		Executor	executor		= Executors.newCachedThreadPool(new DaemonThreadFactory("service-stopper"));
		executor.execute(new Runnable()
		{
			public void run()
			{
				w.stop();			
			}
		});
		while (w.getState() == WrappedProcess.STATE_RUNNING)
		{
			w.getWrapperLogger().info("service waiting " + w.getStringState());
			Thread.sleep(500);
		}
		w.getWrapperLogger().info("service shuting down ");
		w.shutdown();
		// leave running scripts time to terminate
		
			Thread.sleep(5000);
			w.getWrapperLogger().info("service shutdown ");
			w.getWrapperLogger().info("- Win service stop isHaltAppOnWrapper");
		}
		w.getWrapperLogger().info("- Win service stop");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			w.getWrapperLogger().throwing(this.getClass().getName(), "doStop", e);
		}

		
		//System.out.println("+ on stop");
	}

}
