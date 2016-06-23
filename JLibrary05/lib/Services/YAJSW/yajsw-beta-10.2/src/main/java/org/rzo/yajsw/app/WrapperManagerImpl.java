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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.configuration.Configuration;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.rzo.yajsw.Constants;
import org.rzo.yajsw.action.Action;
import org.rzo.yajsw.action.ActionFactory;
import org.rzo.yajsw.config.YajswConfiguration;
import org.rzo.yajsw.config.YajswConfigurationImpl;
import org.rzo.yajsw.controller.Message;
import org.rzo.yajsw.controller.jvm.JVMController;
import org.rzo.yajsw.controller.jvm.MessageDecoder;
import org.rzo.yajsw.controller.jvm.MessageEncoder;
import org.rzo.yajsw.io.CyclicBufferFileInputStream;
import org.rzo.yajsw.io.CyclicBufferFilePrintStream;
import org.rzo.yajsw.io.TeeInputStream;
import org.rzo.yajsw.io.TeeOutputStream;
import org.rzo.yajsw.log.MyLogger;
import org.rzo.yajsw.nettyutils.LoggingFilter;
import org.rzo.yajsw.script.Script;
import org.rzo.yajsw.script.ScriptFactory;
import org.rzo.yajsw.util.Cycler;
import org.rzo.yajsw.util.DaemonThreadFactory;
import org.rzo.yajsw.wrapper.AlphanumComparator;


// TODO: Auto-generated Javadoc
/**
 * The Class WrapperManagerImpl.
 */
public class WrapperManagerImpl implements WrapperManager, Constants, WrapperManagerImplMBean
{


	/** The _port. */
	int							_port				= DEFAULT_PORT;

	/** The _debug. */
	boolean						_debug				= false;

	/** The log. */
	Logger						log					= Logger.getLogger(JVMController.class.getName());

	/** The _started. */
	volatile boolean						_started			= false;

	/** The _key. */
	String						_key;

	/** The _ping interval. */
	int							_pingInterval		= 5;

	/** The connector. */
	ClientBootstrap					connector;

	/** The _session. */
	Channel					_session;

	/** The _stopping. */
	boolean						_stopping			= false;

	/** The _config. */
	Configuration				_config;

	/** The instance. */
	static WrapperManagerImpl	instance;

	/** The _exit code. */
	int							_exitCode			= 0;

	/** The main method. */
	Method						mainMethod			= null;

	/** The main method args. */
	String[]					mainMethodArgs		= null;

	/** The exit on main terminate. */
	int							exitOnMainTerminate	= -1;
	private int	exitOnException = 999;

	/** The _my pid. */
	int							_myPid				= -1;

	boolean						_externalStop		= false;

	String						_groovyScript		= null;

	Cycler						_pinger;
	
	OutputStream			_outStream;
	OutputStream			_errStream;
	
	boolean _appearHanging = false;
	
	boolean _overrideStdErr = false;
	
	boolean _haltAppOnWrapper = false;
	
	Lock _lock = new ReentrantLock();
	Condition _connectEnd = _lock.newCondition();
	Executor executor = Executors.newCachedThreadPool(new DaemonThreadFactory("connector"));
	
	long _startupTimeout = 0;



	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#init(java.lang.String[],
	 * java.lang.ClassLoader)
	 */
	public void init(String[] args, ClassLoader wrapperClassLoader)
	{
		/*
		 * System.out.println(Scheduler.class.getClassLoader());
		 * System.out.println(Configuration.class.getClassLoader());
		 * System.out.flush(); try { Thread.sleep(10000); } catch
		 * (InterruptedException e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); }
		 */
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(wrapperClassLoader);
		instance = this;
		String outFile = System.getProperty("wrapper.teeName");
		String outPath = System.getProperty("wrapper.tmpPath");
		String vStr = System.getProperty("wrapper.console.visible");
		boolean visible = vStr != null && vStr.equals("true");
		if (outFile != null)
		{
			teeSystemStreams(outFile, outPath, visible);
		}
		logJavaInfo(args);

		String preScript = System.getProperty("wrapper.app.pre.script");
		if (preScript != null & !"".equals(preScript))
		{
			Logger logger = new MyLogger();
			logger.addHandler(new ConsoleHandler());
			Script script = ScriptFactory.createScript(preScript, "", null, new String[0], null);
			if (script != null)
				script.execute();
		}

		
		YajswConfigurationImpl config = new YajswConfigurationImpl();
		//config.setDebug(false);
		config.init();

		try
		{
		_overrideStdErr = config.getBoolean("wrapper.java.dump.override", false);
		}
		catch (Exception ex)
		{
			System.out.println("Error getting wrapper.java.dump.override " + ex.getMessage());
		}
		String mainClassName = config.getString("wrapper.java.app.mainclass");
		String jarName = config.getString("wrapper.java.app.jar");
		String groovyScript = config.getString("wrapper.groovy");
		if (mainClassName == null && jarName == null && groovyScript == null)
			mainClassName = config.getString("wrapper.app.parameter.1");
		if (jarName == null && mainClassName == null && groovyScript == null)
		{
			System.out.println("missing main class name or jar file or groovy file. please check configuration");
			return;
		}
		if (jarName != null)
		{
			mainMethod = loadJar(jarName);
		}
		else if (mainClassName != null)
			try
			{
				Class cls = ClassLoader.getSystemClassLoader().loadClass(mainClassName);// Class.forName(mainClassName,
				// currentContext);
				mainMethod = cls.getDeclaredMethod("main", new Class[]
				{ String[].class });
			}
			catch (Exception e)
			{
				System.out.println("error finding main method in class: " + mainClassName + " : " + e.getMessage());
				// log.throwing(WrapperMain.class.getName(), "main", e);
				e.printStackTrace();
				return;
			}
		else
			_groovyScript = groovyScript;

		String stopConfig = config.getString("wrapper.stop.conf");
		if (stopConfig != null)
		{
			File f = new File(stopConfig);
			_externalStop = true;
		}
		System.out.println("external stop " + _externalStop);

		exitOnMainTerminate = config.getInt("wrapper.exit_on_main_terminate", DEFAULT_EXIT_ON_MAIN_TERMINATE);

		exitOnException = config.getInt("wrapper.exit_on_main_exception", DEFAULT_EXIT_ON_MAIN_EXCEPTION);

		mainMethodArgs = getAppParam((Configuration) config);
		setConfiguration((Configuration) config);
		if (_config.getBoolean("wrapper.java.jmx", false))
			registerMBean(config);
		
		String control = _config.getString("wrapper.control", DEFAULT_CONTROL);
		if ("TIGHT".equals(control) || "APPLICATION".equals(control))
			_haltAppOnWrapper = true;
		
		Thread.currentThread().setContextClassLoader(currentClassLoader);

		setKey(_config.getString("wrapper.key"));
		// setDebug(true);
		setPort(_config.getInt("wrapper.port"));
		setPingInterval(_config.getInt("wrapper.ping.interval", Constants.DEFAULT_PING_INTERVAL));
		
		_startupTimeout = _config.getInt("wrapper.startup.timeout", DEFAULT_STARTUP_TIMEOUT) * 1000;
		_debug = _config.getBoolean("wrapper.debug", false);
	}

	private void registerMBean(YajswConfiguration config)
	{
		MBeanServer server = null;
		ArrayList servers = MBeanServerFactory.findMBeanServer(null);
		try
		{
			if (servers != null && servers.size() > 0)
				server = (MBeanServer) servers.get(0);
			if (server != null)
			{
				String name = config.getString("wrapper.console.title");
				if (name == null)
					name = config.getString("wrapper.ntservice.name");
				if (name == null)
					name = "yajsw.noname";
				ObjectName oName = new ObjectName("Wrapper", "name", name);
				server.registerMBean(this, oName);
				// System.out.println("found mbean server: " +
				// server.toString());
			}
			else
				System.out.println("ERROR: no mbean server found ");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#getMainMethod()
	 */
	public Method getMainMethod()
	{
		return mainMethod;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#getMainMethodArgs()
	 */
	public Object[] getMainMethodArgs()
	{
		return mainMethodArgs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#isExitOnMainTerminate()
	 */
	public int getExitOnMainTerminate()
	{
		if (_debug)
		System.out.println("exit on main terminate " + exitOnMainTerminate);
		return exitOnMainTerminate;
	}
	
	public int getExitOnException()
	{
		if (_debug)
		System.out.println("exit on main exception " + exitOnException);
		return exitOnException;
	}


	/**
	 * Load jar.
	 * 
	 * @param jarName
	 *            the jar name
	 * 
	 * @return the method
	 */
	private Method loadJar(String jarName)
	{
		URL url = null;
		try
		{
			url = new File(jarName).toURI().toURL();
		}
		catch (MalformedURLException e2)
		{
			e2.printStackTrace();
			return null;
		}
		Manifest manifest;
		try
		{
			manifest = new JarFile(new File(jarName)).getManifest();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return null;
		}
		Attributes attr = manifest.getMainAttributes();

		String cl = attr.getValue("Class-Path");
		ClassLoader loader = null;
		if (cl != null)
		{
			ArrayList classpath = new ArrayList();
			String[] clArr = cl.split(" ");
			for (int i = 0; i < clArr.length; i++)
			{
				String file = clArr[i];
				File myFile;
				try
				{
					myFile = new File(file);
					classpath.add(myFile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			URL[] urlsArr = new URL[classpath.size()];
			int i = 0;
			for (Iterator it = classpath.iterator(); it.hasNext(); i++)
				try
				{
					urlsArr[i] = ((File) it.next()).toURI().toURL();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

			loader = new URLClassLoader(urlsArr, ClassLoader.getSystemClassLoader());
		}
		if (loader == null)
			loader = ClassLoader.getSystemClassLoader();

		String mainClassName = attr.getValue("Main-Class");
		if (mainClassName == null)
			return null;
		Method mainMethod = null;
		try
		{
			Class cls = loader.loadClass(mainClassName);// cl.loadClass(mainClassName);
			mainMethod = cls.getDeclaredMethod("main", new Class[]
			{ String[].class });
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return mainMethod;
	}

	private File createRWfile(String path, String fname)
	{
		File result = new File(path, fname);
		try
		{
			if (!result.exists())
			{
				// result.createNewFile();
			}
			String name = result.getCanonicalPath();
			// System.out.println("chmod 777 "+name);
			// Runtime.getRuntime().exec("chmod 777 "+name);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * Tee system streams.
	 * 
	 * @param outFile
	 *            the out file
	 * @param path
	 *            the path
	 * @param visible
	 *            the visible
	 */
	private void teeSystemStreams(String outFile, String path, boolean visible)
	{
		File fOut = createRWfile(path, "out_" + outFile);
		// if (fOut.exists())
		// fOut.delete();
		File fErr = createRWfile(path, "err_" + outFile);
		// if (fErr.exists())
		// fErr.delete();
		File fIn = createRWfile(path, "in_" + outFile);

		try
		{
			PrintStream wrapperOut = (PrintStream) new CyclicBufferFilePrintStream(fOut);
			TeeOutputStream newOut = (TeeOutputStream) new TeeOutputStream();
			newOut.connect(wrapperOut);
			// pipe output to console only if it is visible
			if (visible)
				newOut.connect(System.out);
			_outStream = wrapperOut;
			System.setOut(new PrintStream(newOut));
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

		try
		{

			PrintStream wrapperErr = (PrintStream) new CyclicBufferFilePrintStream(fErr);
			TeeOutputStream newErr = (TeeOutputStream) new TeeOutputStream();
			newErr.connect(wrapperErr);
			// pipe output to console only if it is visible
			if (visible)
				newErr.connect(System.err);
			_errStream = newErr;
			System.setErr(new PrintStream(newErr));
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

		try
		{
			CyclicBufferFileInputStream wrapperIn = new CyclicBufferFileInputStream(fIn);
			TeeInputStream newIn = (TeeInputStream) new TeeInputStream();
			newIn.connect(wrapperIn);
			newIn.connect(System.in);
			System.setIn(newIn);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Log java info.
	 * 
	 * @param args
	 *            the args
	 */
	private void logJavaInfo(String[] args)
	{
		if (_debug)
		{
		System.out.println("user name=" + System.getProperty("user.name"));
		System.out.println("working dir=" + System.getProperty("user.dir"));
		System.out.println("java version=" + System.getProperty("java.version"));
		System.out.println("class path=" + System.getProperty("java.class.path"));
		System.out.println("library path=" + System.getProperty("java.library.path"));
		}
		String[] files = System.getProperty("java.class.path").split(File.pathSeparator);
		for (int i = 0; i < files.length; i++)
		{
			File f = new File(files[i]);
			if (!f.exists())
				System.out.println("Classpath File not found: " + files[i]);
		}

		if (_debug)
		{
		String argsStr = "Application args: ";
		if (args != null && args.length > 0)
			for (int i = 0; i < args.length; i++)
			{
				argsStr += args[i] + " ";
			}
		else
			argsStr += "no args";
		System.out.println(argsStr);
		}

	}

	/**
	 * Gets the app param.
	 * 
	 * @param config
	 *            the config
	 * 
	 * @return the app param
	 */
	private String[] getAppParam(Configuration config)
	{
		ArrayList result = new ArrayList();
		ArrayList keys = new ArrayList();
		for (Iterator it = config.getKeys("wrapper.app.parameter"); it.hasNext();)
		{
			keys.add(it.next());
		}
		Collections.sort(keys, new AlphanumComparator());
		for (Iterator it = keys.listIterator(); it.hasNext();)
		{
			result.add(config.getString((String) it.next()));
		}
		String[] args = new String[result.size()];
		int i = 0;
		for (Iterator it = result.iterator(); it.hasNext(); i++)
		{
			args[i] = (String) it.next();
		}
		if (_debug)
		{
		System.out.println("args: ");
		for (String arg : args)
			System.out.println(arg);
		}
		return args;
	}

	/**
	 * Sets the configuration.
	 * 
	 * @param config
	 *            the new configuration
	 */
	void setConfiguration(Configuration config)
	{
		_config = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#start()
	 */
	public void start()
	{

		connector = new ClientBootstrap(
                new OioClientSocketChannelFactory(
                		//executor,
                		executor));
		// add logging
		if (_debug)
		{
			connector.getPipeline().addLast("logger", new LoggingFilter(log, "app"));
			log.info("Logging ON");
		}
		
		// add a framer to split incoming bytes to message chunks 
		connector.getPipeline().addLast("framer", new DelimiterBasedFrameDecoder(
	            8192, true, Delimiters.nulDelimiter()));

		// add message coding
		connector.getPipeline().addLast("messageEncoder", new MessageEncoder());
		connector.getPipeline().addLast("messageDecoder", new MessageDecoder());

		// pinger is a cycler with high priority threads
		// sends ping messages within a ping interval
		_pinger = new Cycler(getPingInterval(), 0, Executors.newCachedThreadPool(new DaemonThreadFactory("pinger", Thread.MAX_PRIORITY)),
				new Runnable()
				{
				long start = System.currentTimeMillis();
					public void run()
					{
						if (_session != null && _session.isConnected() && !_stopping && !_appearHanging)
						{
							_session.write(new Message(Constants.WRAPPER_MSG_PING, null));
							start = System.currentTimeMillis();
						}
						else if (_haltAppOnWrapper && (System.currentTimeMillis()-start) > _startupTimeout)
						{
							System.out.println("no connection to wrapper during "+(_startupTimeout/1000)+"seconds -> System.exit(-1)");
							System.out.println("if this is this is due to server overload consider increasing wrapper.startup.timeout");
								System.exit(-1);								
						}
					}
				});
		_pinger.start();

		// connect
		connector.setOption("remoteAddress", new InetSocketAddress("127.0.0.1", _port));
		connector.setOption("connectTimeoutMillis", 10 * 1000);
		connector.setOption("reuseAddress", true);
		connector.setOption("tcpNoDelay", true);
		connector.getPipeline().addLast("handler", new WrapperHandler());
		
		// handler should process messages in a separate thread


		reconnect();
		/*
		 * try { if (_config.getInt("wrapper.action.port") > 0) { _actionHandler
		 * = new ActionHandler(this, _config); _actionHandler.start(); } } catch
		 * (Exception ex) { log.info("could not start action handler " +
		 * ex.getMessage()); }
		 */

	}

	/**
	 * Reconnect.
	 */
	private void reconnect()
	{
		// try connecting, if we could not sleep then retry
		while (!_started)
		{
			if (_debug)
				log.info("connecting to port " + _port);
			final ChannelFuture future1 = connector.connect();
			try
			{
				future1.await();
				_started = future1.isSuccess();
			}
			catch (InterruptedException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			/*
			executor.execute(new Runnable()
			{

				public void run()
				{
					future1.addListener(new ChannelFutureListener()
					{
						public void operationComplete(ChannelFuture future) throws Exception
						{
							_lock.lock();
							System.out.println("future" + future.isSuccess());
							_started = future.isSuccess();
							_connectEnd.signal();
							_lock.unlock();
							
						}
					});
				}
				
			});
			
			_lock.lock();
			try
			{
				_connectEnd.await();
			}
			catch (InterruptedException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			_lock.unlock();
			System.out.println("started "+_started);
			*/
			
			
			if (_started)
				future1.getChannel().write(new Message(Constants.WRAPPER_MSG_KEY, _key));
			else
				try
				{
					if (_debug)
						System.out.println("connection failed -> sleep then retry");
					_started = false;
					Thread.sleep(5000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
		}
	}

	/**
	 * The Class WrapperHandler.
	 */
	@ChannelPipelineCoverage("one")
	class WrapperHandler extends SimpleChannelHandler
	{

		@Override
	    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
		{
			if (_debug)
				System.out.println("session closed: try again");
			_started = false;
			_session = null;
			if (!_stopping)
			{
				executor.execute(new Runnable()
				{
					public void run()
					{
				try
				{
					Thread.sleep(5000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				reconnect();
					}
				});
			}
		}

		@Override
	    public void messageReceived(
	            ChannelHandlerContext ctx, MessageEvent e) throws Exception 
		{
			if (_stopping)
				return;
			Channel session = ctx.getChannel();
			Message msg = (Message) e.getMessage();
			if (msg.getCode() == Constants.WRAPPER_MSG_STOP)
				try
				{
					System.out.println("wrapper manager received stop command");
					_stopping = true;
					if (session != null)
						session.close();
					// Thread.sleep(100);
					if (msg.getMessage() != null && msg.getMessage().length() > 0)
						try
						{
							_exitCode = Integer.parseInt(msg.getMessage());
						}
						catch (Exception ex)
						{
							// DO NOTHING
						}
					if (!_externalStop)
						System.exit(_exitCode);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			else if (msg.getCode() == Constants.WRAPPER_MSG_OKKEY)
			{
				_session = session;
				try
				{
					_myPid = Integer.parseInt(msg.getMessage());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
			else if (msg.getCode() == Constants.WRAPPER_MSG_THREAD_DUMP)
			{
				threadDump();
			}
		}
		
		@Override
	    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
        throws Exception
	    {
			if (_debug)
				System.out.println(e.getCause());
	    	
	    }


	}

	/**
	 * Gets the port.
	 * 
	 * @return the port
	 */
	int getPort()
	{
		return _port;
	}

	/**
	 * Sets the port.
	 * 
	 * @param port
	 *            the new port
	 */
	public void setPort(int port)
	{
		_port = port;
	}

	/**
	 * Checks if is debug.
	 * 
	 * @return true, if is debug
	 */
	boolean isDebug()
	{
		return _debug;
	}

	/**
	 * Sets the debug.
	 * 
	 * @param debug
	 *            the new debug
	 */
	void setDebug(boolean debug)
	{
		_debug = debug;
	}

	/**
	 * Gets the key.
	 * 
	 * @return the key
	 */
	String getKey()
	{
		return _key;
	}

	/**
	 * Sets the key.
	 * 
	 * @param key
	 *            the new key
	 */
	public void setKey(String key)
	{
		_key = key;
	}

	/**
	 * Checks if is started.
	 * 
	 * @return true, if is started
	 */
	boolean isStarted()
	{
		return _started;
	}

	/**
	 * Gets the ping interval.
	 * 
	 * @return the ping interval
	 */
	int getPingInterval()
	{
		return _pingInterval;
	}

	/**
	 * Sets the ping interval.
	 * 
	 * @param pingInterval
	 *            the new ping interval
	 */
	void setPingInterval(int pingInterval)
	{
		_pingInterval = pingInterval * 1000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#stop()
	 */
	public void stop()
	{
		if (_session != null)
			while (_session != null && !_stopping)
			{
				_session.write(new Message(Constants.WRAPPER_MSG_STOP, null));
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		else
			System.exit(0);

	}

	/**
	 * Stop timer.
	 */
	public void stopTimer()
	{
		if (_session != null)
			while (_session != null && !_stopping)
			{
				_session.write(new Message(Constants.WRAPPER_MSG_STOP_TIMER, null));
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
	}

	/**
	 * Restart.
	 */
	public void restart()
	{
		if (_session != null)
			while (_session != null && !_stopping)
			{
				_session.write(new Message(Constants.WRAPPER_MSG_RESTART, null));
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		else
			System.out.println("not connected to wrapper -> cannot send restart command");
	}

	/**
	 * Instance.
	 * 
	 * @return the wrapper manager impl
	 */
	public static WrapperManagerImpl instance()
	{
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.WrapperManager#getPid()
	 */
	public int getPid()
	{
		return _myPid;
	}

	public boolean isControlledByWrapper()
	{
		return _started;
	}

	public boolean isLaunchedAsService()
	{
		return _config.getBoolean("wrapper.service", false);
	}

	public String getGroovyScript()
	{
		return _groovyScript;
	}

	public void threadDump()
	{
		Message m = new Message(Constants.WRAPPER_MSG_THREAD_DUMP, null);
		Action a = ActionFactory.getAction(m);
		try
		{
			if (_overrideStdErr)
				a.execute(m, _session, new PrintStream(_errStream));
			else
				a.execute(m, _session, System.err);
				
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws MalformedURLException
	{
		String name = "c:/test test/start.jar";
		System.out.println(new File(name).exists());
		System.out.println(new File(new File(name).toURI().getPath()).exists());
		WrapperManager wm = new WrapperManagerImpl();
		((WrapperManagerImpl) wm).loadJar("c:/test test/start.jar");
		synchronized (wm)
		{
			wm.threadDump();
		}
	}

	public boolean isAppearHanging()
	{
		return _appearHanging;
	}

	public void setAppearHanging(boolean appearHanging)
	{
		_appearHanging = appearHanging;
	}

}