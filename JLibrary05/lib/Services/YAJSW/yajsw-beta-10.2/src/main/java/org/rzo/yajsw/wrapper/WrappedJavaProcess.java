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
package org.rzo.yajsw.wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.rzo.yajsw.Constants;
import org.rzo.yajsw.boot.WrapperLoader;
import org.rzo.yajsw.controller.AbstractController.ControllerListener;
import org.rzo.yajsw.controller.jvm.JVMController;
import org.rzo.yajsw.log.MyLogger;
import org.rzo.yajsw.os.JavaHome;
import org.rzo.yajsw.os.OperatingSystem;
import org.rzo.yajsw.script.Script;
import org.rzo.yajsw.script.ScriptFactory;
import org.rzo.yajsw.util.FileUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class WrappedJavaProcess.
 */
public class WrappedJavaProcess extends AbstractWrappedProcess
{

	/** The _key. */
	String	_key;

	/** The _tee name. */
	String	_teeName;

	/** The _java pid file. */
	File	_javaPidFile;
	
	boolean _initController = false;

	public void init()
	{
		super.init();
		_key = "" + new Random(System.currentTimeMillis()).nextLong();
		_config.setProperty("wrapper.key", _key);
		if (_controller == null)
		{
			_controller = new JVMController(this);
			configController();
		}

	}

	protected void setState(int state)
	{
		super.setState(state);
		if (state == STATE_IDLE)
		{
			removeJavaPidFile();
		}

	}

	/**
	 * Config process.
	 */
	void configProcess()
	{
		// _osProcess.destroy();
		if (pipeStreams())
		{
			// _osProcess.setPipeStreams(true, false);
			_teeName = _key + "$" + System.currentTimeMillis();
			_config.setProperty("wrapper.teeName", _teeName);
			_osProcess.setTeeName(_teeName);
			_tmpPath = _config.getString("wrapper.tmp.path");
			if (_tmpPath == null)
				_tmpPath = System.getProperty("java.io.tmpdir");
			if (_tmpPath == null)
				_tmpPath = "/tmp";
			File t = new File(_tmpPath);
			if (!t.exists())
				t.mkdirs();
			_tmpPath = t.getAbsolutePath();
			_osProcess.setTmpPath(_tmpPath);
		}

		JavaHome javaHome = OperatingSystem.instance().getJavaHome(_config);
		String java = javaHome.findJava();
		List jvmOptions = jvmOptions();
		List wrapperOptions = wrapperOptions();
		String mainClass = getMainClass();
		List command = new ArrayList();
		command.add(java);
		command.addAll(jvmOptions);
		command.addAll(wrapperOptions);
		command.add(mainClass);
		String[] arrCmd = new String[command.size()];
		for (int i=0; i<arrCmd.length; i++)
			arrCmd[i] = (String)command.get(i);
		_osProcess.setCommand(arrCmd);
		_osProcess.setPipeStreams(false, false);
		super.configProcess();
	}

	protected String getMainClass()
	{
		return _config.getString("wrapper.java.mainclass", "org.rzo.yajsw.app.WrapperJVMMain");
	}

	/**
	 * Wrapper options.
	 * 
	 * @return the string
	 */
	private List wrapperOptions()
	{
		ArrayList result = new ArrayList();
		JVMController controller = (JVMController) _controller;

		for (Iterator it = _config.getSystemConfiguration().getKeys("wrapper"); it.hasNext();)
		{
			String key = (String) it.next();
			if (("wrapper.service".equals(key) || "wrapper.console.visible".equals(key)) && _config.getBoolean("wrapper.service", false))
				continue;
			if ("wrapper.config".equals(key))
			{
				result.add(checkValue("-D" + key + "="+_config.getCachedPath()));
			}
			else
			result.add(checkValue("-D" + key + "="+_config.getProperty(key).toString()));
		}
		result.add("-Dwrapper.port=" + controller.getPort());
		result.add("-Dwrapper.key=" + controller.getKey());
		result.add("-Dwrapper.teeName=" + _teeName);
		result.add("-Dwrapper.tmpPath=" + _tmpPath);
		
		String preScript = _config.getString("wrapper.app.pre.script", null);
		if (preScript != null & !"".equals(preScript))
		try {
			File f = new File(preScript);
			if (!f.exists())
				getWrapperLogger().warning("app.pre.script not found: "+preScript);
			else
			{
				preScript = checkValue(f.getCanonicalPath());
				result.add("-Dwrapper.app.pre.script=" + preScript);
			}
		}
		catch (Exception ex)
		{
			getWrapperLogger().throwing("WrappedJavaProcess", "wrapperOptions", ex);
		}


		return result;
	}

	/**
	 * Jvm options.
	 * 
	 * @return the string
	 */
	private List jvmOptions()
	{
		ArrayList result = new ArrayList();
		result.add("-classpath");
		StringBuffer sb = new StringBuffer();
		sb.append(WrapperLoader.getWrapperAppJar().trim());
		StringBuilder appCp = getAppClassPath(_config.getString("wrapper.working.dir", "."), _config.getKeys("wrapper.java.classpath"));
		if (appCp != null && appCp.length() > 0)
		{
			sb.append(PATHSEP);
			sb.append(appCp);
		}
		result.add(checkValue(sb.toString()));
		boolean hasXrs = false;
		boolean hasXmx = false;
		boolean hasXms = false;
		for (Iterator it = _config.getKeys("wrapper.java.additional"); it.hasNext();)
		{
			String key = (String) it.next();
			String value = _config.getString(key);
			result.add(checkValue(value));
			hasXrs |= value.contains("-Xrs");
			hasXmx |= value.contains("-Xmx");
			hasXms |= value.contains("-Xms");
		}
		sb = new StringBuffer();
		if (_config.getKeys("wrapper.java.library.path").hasNext())
		{
			sb.append("-Djava.library.path=");
			for (Iterator it = _config.getKeys("wrapper.java.library.path"); it.hasNext();)
			{
				String key = (String) it.next();
				sb.append(_config.getString(key));
				if (it.hasNext())
					sb.append(PATHSEP);
			}
			result.add(checkValue(sb.toString()));
		}

		if (_config.getBoolean("wrapper.service", false) && !hasXrs)
		{
			result.add("-Xrs");
		}
		if (_config.getBoolean("wrapper.service", false))
		{
			result.add("-Dwrapper.service=true");
			result.add("-Dwrapper.console.visible=false");
		}
		else if (_config.getBoolean("wrapper.console.visible", Constants.DEFAULT_CONSOLE_VISIBLE))
			result.add("-Dwrapper.console.visible=true");

		long xmx = 0;
		long xmxr = 0;
		long xms = 0;
		long xmsr = 0;
		OperatingSystem.instance().systemInformation().setLogger(this.getWrapperLogger());
		long totalRAM = OperatingSystem.instance().systemInformation().totalRAM();
		if (!hasXms)
		{
			try
			{
				xms = _config.getLong("wrapper.java.initmemory", 0);
				xmsr = _config.getLong("wrapper.java.initmemory.relative", 0);
			}
			catch (Exception ex)
			{
				System.out.println("error in wrapper.java.initmemory " + ex.getMessage());
			}
			if (xmsr > 0 && totalRAM > 0)
				xms = (totalRAM * xmsr) / 100 / (1024 * 1024);
			if (xms > 0)
			{
				result.add("-Xms" + xms + "m");
			}
		}
		if (!hasXmx)
		{
			try
			{
				xmx = _config.getLong("wrapper.java.maxmemory", 0);
				xmxr = _config.getLong("wrapper.java.maxmemory.relative", 0);
			}
			catch (Exception ex)
			{
				System.out.println("error in wrapper.java.maxmemory " + ex.getMessage());
			}
			if (xmxr > 0 && totalRAM > 0)
				xmx = (totalRAM * xmxr) / 100 / (1024 * 1024);
			if (xmx > 0)
			{
				if (xmx < xms)
					xmx = xms;
				if (xmx < 3)
					xmx = 3;
				result.add("-Xmx" + xmx + "m");
			}
		}
		int port = _config.getInt("wrapper.java.debug.port", -1);
		if (port != -1)
		{
			result.add("-Xdebug");
			result.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address="+port);
		}
		return result;
	}

	// call to java "-Ddir=c:\" will cause a parse exception in the java launcher
	private String checkValue(String value)
	{
		value = value.trim();
		if (value.endsWith("\\") && ! value.endsWith("\\\\"))
			value += "\\";
		return value;
	}

	/**
	 * Gets the app class path.
	 * 
	 * @param workingDir
	 *            the working dir
	 * @param config
	 *            the config
	 * 
	 * @return the app class path
	 */
	private StringBuilder getAppClassPath(String workingDir, Iterator keys)
	{
		List configList = new ArrayList();
		for (Iterator it = keys; it.hasNext();)
		{
			configList.add(it.next());
		}
		Collections.sort(configList, new AlphanumComparator());
		List files = new ArrayList();
		for (Iterator it = configList.listIterator(); it.hasNext();)
		{
			String file = _config.getString((String) it.next());
			files.addAll(FileUtils.getFiles(workingDir, file));
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator it = files.iterator(); it.hasNext();)
		{
			try
			{
				sb.append(((File) it.next()).getCanonicalPath());
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (it.hasNext())
				sb.append(PATHSEP);

		}

		return sb;
	}

	/**
	 * Config controller.
	 */
	void configController()
	{
		JVMController controller = (JVMController) _controller;

		controller.setLogger(getWrapperLogger());
		controller.setKey(_config.getString("wrapper.key"));
		if (_config.containsKey("wrapper.port"))
		{
			controller.setMinPort(_config.getInt("wrapper.port"));
			controller.setMaxPort(_config.getInt("wrapper.port"));
		}
		else
		{
				controller.setMinPort(_config.getInt("wrapper.port.min", Constants.DEFAULT_PORT));
				controller.setMaxPort(_config.getInt("wrapper.port.max", 65535));
		}
			

		controller.setStartupTimeout(_config.getInt("wrapper.startup.timeout", DEFAULT_STARTUP_TIMEOUT) * 1000);
		controller.setPingTimeout(_config.getInt("wrapper.ping.timeout", DEFAULT_PING_TIMEOUT) * 1000);
		if (! _initController)
		{
		ControllerListener restartHandler = new ControllerListener()
		{
			public void fire()
			{
				if (_state == STATE_RESTART_STOP)
					return;
				if (allowRestart() && exitCodeRestart())
				{
					restartInternal();
				}
				else
				{
					if (_debug)
					{
						getWrapperLogger().info("giving up after " + _restartCount + " retries");
					}
					if (_state != STATE_USER_STOP)
						setState(STATE_ABORT);
					if (!_exiting)
					stop();
					setState(STATE_IDLE);

				}

			}

		};
		controller.addListener(JVMController.STATE_STARTUP_TIMEOUT, restartHandler);
		controller.addListener(JVMController.STATE_PING_TIMEOUT, restartHandler);
		controller.addListener(JVMController.STATE_PROCESS_KILLED, restartHandler);
		controller.init();
		_initController = true;
		}
	}

	void postStart()
	{
		saveJavaPidFile();

	}

	// test main
	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		WrappedProcess[] w = new WrappedProcess[20];
		for (int i = 0; i < w.length; i++)
		{
			w[i] = new WrappedJavaProcess();
			w[i].getLocalConfiguration().setProperty("wrapper.config", "conf/wrapper.helloworld.conf");
			w[i].getLocalConfiguration().setProperty("wrapper.debug", "true");
			w[i].setUseSystemProperties(false);
			w[i].init();
		}
		boolean done = false;
		while (!done)
		{
			// done = true;
			for (int i = 0; i < w.length; i++)
			{
				System.out.println("starting " + i);
				w[i].start();
			}
			try
			{
				Thread.sleep(5000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			for (int i = 0; i < w.length; i++)
			{
				System.out.println("stopping " + i);
				w[i].stop();
			}
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Save java pid file.
	 */
	void saveJavaPidFile()
	{
		String file = _config.getString("wrapper.java.pidfile");
		if (file != null)
		{
			try
			{
				_javaPidFile = new File(file);
				if (!_javaPidFile.exists())
					_javaPidFile.createNewFile();
				FileWriter out = new FileWriter(_javaPidFile, false);
				out.write("" + getAppPid());
				out.flush();
				out.close();
				if (_debug)
					getWrapperLogger().info("created jva.pid file " + _javaPidFile.getAbsolutePath());
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Removes the java pid file.
	 */
	void removeJavaPidFile()
	{
		if (_javaPidFile != null)
		{
			try
			{
				_javaPidFile.delete();

				if (_debug)
					getWrapperLogger().info("removed java.pid file " + _javaPidFile.getAbsolutePath());
				_javaPidFile = null;
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reconnect.
	 * 
	 * @param pid
	 *            the pid
	 * 
	 * @return true, if successful
	 */
	public boolean reconnect(int pid)
	{
		if (_state != STATE_IDLE)
			return false;

		_osProcess = OperatingSystem.instance().processManagerInstance().getProcess(pid);
		if (_osProcess == null)
			return false;
		String cmd = _osProcess.getCommand();
		if (cmd == null)
			return false;
		String key = getPropertyFromCommandLine("wrapper.key=[^ \"]*", cmd);
		if (key == null)
			return false;
		String port = getPropertyFromCommandLine("wrapper.port=[^ \"]*", cmd);
		if (port == null)
			return false;
		String configFile = getPropertyFromCommandLine("wrapper.config=[^ \"]*", cmd);
		String teeName = getPropertyFromCommandLine("wrapper.teeName=[^ \"]*", cmd);
		String tmpPath = getPropertyFromCommandLine("wrapper.tmpPath=[^ \"]*", cmd);

		_localConfiguration.setProperty("wrapper.key", key);
		_localConfiguration.setProperty("wrapper.port", port);
		_localConfiguration.setProperty("wrapper.teeName", teeName);
		_localConfiguration.setProperty("wrapper.tmpPath", tmpPath);
		if (configFile != null)
			_localConfiguration.setProperty("wrapper.config", configFile);

		super.init();
		_osProcess.setTeeName(teeName);
		_osProcess.setTmpPath(tmpPath);
		_osProcess.reconnectStreams();

		if (_controller == null)
			_controller = new JVMController(this);

		JVMController controller = (JVMController) _controller;

		//controller.setDebug(true);
		configController();

		_firstRestartTime = System.currentTimeMillis();

		//controller.setDebug(true);
		controller.start();
		controller.processStarted();
		setState(STATE_RUNNING);

		boolean result = controller.waitFor(_config.getInt("wrapper.ping.timeout", DEFAULT_PING_TIMEOUT) * 1000);
		if (result)
		{
			// wait for stream to be available
			for (int i = 0; i < 5 && _osProcess.getInputStream() == null; i++)
				try
				{
					Thread.sleep(200);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
				Map triggerActions = getTriggerActions();
				Map regexTriggerActions = getRegexTriggerActions();
				Map missingTriggerActions = getMissingTriggerActions();
				Map missingRegexTriggerActions = getMissingRegexTriggerActions();

			_gobler_in = new Gobler(_osProcess.getInputStream(), getAppLogger(), triggerActions, regexTriggerActions, missingTriggerActions, missingRegexTriggerActions, "OUTPUT "
					+ _osProcess.getPid(), _osProcess.getPid());
			_gobler_err = new Gobler(_osProcess.getErrorStream(), getAppLogger(), triggerActions, regexTriggerActions, missingTriggerActions, missingRegexTriggerActions, "ERROR "
					+ _osProcess.getPid(), _osProcess.getPid());
			executor.execute(_gobler_err);
			executor.execute(_gobler_in);
			setState(STATE_RUNNING);
			saveJavaPidFile();
			saveLockFile();
		}
		return result;
	}

	/**
	 * Gets the property from command line.
	 * 
	 * @param pattern
	 *            the pattern
	 * @param cmd
	 *            the cmd
	 * 
	 * @return the property from command line
	 */
	private String getPropertyFromCommandLine(String pattern, String cmd)
	{
		String result = null;
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(cmd);
		if (m.find())
			result = m.group();
		if (result != null && result.length() > 0)
			return result.substring(result.indexOf("=") + 1);
		return null;
	}

	/**
	 * Gets the tee name.
	 * 
	 * @return the tee name
	 */
	public String getTeeName()
	{
		return _teeName;
	}

	/**
	 * Request thread dump.
	 */
	public void requestThreadDump()
	{
		if (_controller != null)
		{
			JVMController controller = (JVMController) _controller;
			controller.requestThreadDump();
		}
	}

	void stopController(int timeout)
	{
		JVMController controller = (JVMController) _controller;
		controller.stop(JVMController.STATE_USER_STOP);
	}

	public String getType()
	{
		return "Java-" + super.getType();
	}
	
}