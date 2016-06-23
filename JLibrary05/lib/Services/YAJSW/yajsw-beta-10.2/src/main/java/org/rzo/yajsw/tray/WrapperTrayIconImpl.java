package org.rzo.yajsw.tray;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.rzo.yajsw.util.DaemonThreadFactory;
import org.rzo.yajsw.wrapper.AbstractWrappedProcessMBean;
import org.rzo.yajsw.wrapper.StateChangeListener;
import org.rzo.yajsw.wrapper.WrappedJavaProcess;
import org.rzo.yajsw.wrapper.WrappedProcess;
import org.rzo.yajsw.wrapper.WrappedService;

public class WrapperTrayIconImpl implements WrapperTrayIcon
{

	Image							iconRunning;
	Image							iconIdle;
	Image							iconElse;
	Image							iconOffline;
	TrayIcon						ti;
	Image							currentImage	= iconIdle;
	String							toolTipPrefix;
	String							currentToolTip;
	final SystemTray				tray			= SystemTray.getSystemTray();
	boolean							init			= false;
	Console							_console		= null;
	volatile AbstractWrappedProcessMBean					_process;
	protected static final Executor	executor		= Executors.newCachedThreadPool(new DaemonThreadFactory("console"));
	volatile boolean							stop			= false;
	int								_currentState	= WrappedProcess.STATE_IDLE;
	JMenuItem						_stopItem		= new JMenuItem();
	JMenuItem						_closeItem		= new JMenuItem();
	JMenuItem						_startItem		= new JMenuItem();
	JMenuItem						_restartItem	= new JMenuItem();
	JMenuItem						_consoleItem	= new JMenuItem();
	JMenuItem						_stopTimerItem	= new JMenuItem();
	JMenuItem						_threadDumpItem	= new JMenuItem();
	JMenuItem						_exitItem		= new JMenuItem();
	JMenuItem						_exitWrapperItem		= new JMenuItem();
	JMenuItem						_threadDumpWrapperItem	= new JMenuItem();
	JMenuItem						_closeConsoleItem	= new JMenuItem();
	JMenuItem						_startServiceItem	= new JMenuItem();
	JMenuItem						_responseItem	= new JMenuItem();
	
	String _inquireMessage = null;

	public WrapperTrayIconImpl(String name, String icon)
	{
		if (!SystemTray.isSupported())
		{
			System.out.println("SystemTray not supported on this platform");
			return;
		}
		
		toolTipPrefix = name + " - ";

		InputStream f = null;
		try
		{
			f = getImage(icon);
			ti = new TrayIcon(createColorImage(f, null, null));
			ti.setImageAutoSize(true);

			Dimension d = ti.getSize();
			f = getImage(icon);
			iconRunning = createColorImage(f, Color.GREEN, d);
			f = getImage(icon);
			iconIdle = createColorImage(f, Color.RED, d);
			f = getImage(icon);
			iconElse = createColorImage(f, Color.ORANGE, d);
			f = getImage(icon);
			iconOffline = createColorImage(f, Color.BLACK, d);
		}
		catch (Exception ex)
		{
			System.out.println("System Tray: file type not supported -> abort");
			return;
		}

		ti = new TrayIcon(iconIdle);
		/*
		process.addStateChangeListener(new StateChangeListener()
		{
			public void stateChange(int newState, int oldState)
			{
				if (newState == WrappedProcess.STATE_SHUTDOWN)
				{
					synchronized (tray)
					{
						tray.remove(ti);
					}

					if (!_process.getType().endsWith("Service"))
						Runtime.getRuntime().halt(0);
					return;
				}
				showState(newState);
			}
		});
		*/
		ti.setImageAutoSize(true);

		final JPopupMenu popup = new JPopupMenu();
		_exitItem.setAction(new AbstractAction("Exit", createImageIcon("/resources/exit.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				popup.setVisible(false);
				stop = true;
				synchronized (tray)
				{
					tray.remove(ti);
				}
			}

		});
		_stopItem.setAction(new AbstractAction("Stop", createImageIcon("/resources/stop.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				executor.execute(new Runnable()
				{

					public void run()
					{
						if (_process != null)
						_process.stop();
					}
					
				});
				popup.setVisible(false);
			}

		});
		_closeItem.setAction(new AbstractAction("Close Popup", createImageIcon("/resources/close.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				popup.setVisible(false);
			}
		});

		_startItem.setAction(new AbstractAction("Start", createImageIcon("/resources/start.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
				_process.start();
				popup.setVisible(false);
			}
		});
		_restartItem.setAction(new AbstractAction("Restart", createImageIcon("/resources/restart.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
				_process.restart();
				popup.setVisible(false);
			}

		});
		_consoleItem.setAction(new AbstractAction("Console", createImageIcon("/resources/console.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
				openConsole();
				popup.setVisible(false);
			}

		});
		_threadDumpItem.setAction(new AbstractAction("Thread Dump", createImageIcon("/resources/lightning.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
					_process.threadDump();
				popup.setVisible(false);
			}
		});

		_stopTimerItem.setAction(new AbstractAction("Stop Timer/Condition", createImageIcon("/resources/clock_stop.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
				{
				_process.stopTimerCondition();
				if (_console != null)
				{
					_console.setTimer(_process.isTimerActive());
					_console.setCondition(_process.isConditionActive());
				}
				}
				popup.setVisible(false);
			}
		});
		_exitWrapperItem.setAction(new AbstractAction("Stop Wrapper", createImageIcon("/resources/exitWrapper.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
					executor.execute(new Runnable()
					{
						public void run()
						{
						if (_process != null)
							_process.stopWrapper();	
						}
					});
				popup.setVisible(false);
			}

		});

		_threadDumpWrapperItem.setAction(new AbstractAction("TDump Wrapper", createImageIcon("/resources/lightning.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null)
				_process.wrapperThreadDump();
				popup.setVisible(false);
			}

		});

		_closeConsoleItem.setAction(new AbstractAction("Close Console")
		{
			public void actionPerformed(ActionEvent e)
			{
					closeConsole();
					popup.setVisible(false);
			}

		});

		_startServiceItem.setAction(new AbstractAction("Start Service", createImageIcon("/resources/startService.png"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process == null)
				{
				WrappedService w = new WrappedService();
				w.init();
				w.start();
				}
				popup.setVisible(false);
			}

		});

		_responseItem.setAction(new AbstractAction("Response", createImageIcon("/resources/Help16.gif"))
		{
			public void actionPerformed(ActionEvent e)
			{
				if (_process != null && _inquireMessage != null)
				{
				String message = _inquireMessage;
				String s = (String)JOptionPane.showInputDialog(
	                    message,
	                    "");
				if (s != null && _process != null)
				{
					_process.setInquireResponse(s);
					_inquireMessage = null;
			
				}

				}
				popup.setVisible(false);
			}

		});

		popup.add(_closeItem);
		popup.add(_startItem);
		popup.add(_stopItem);
		popup.add(_restartItem);
		popup.add(_consoleItem);
		popup.add(_responseItem);
		//popup.add(_threadDumpWrapperItem);
		popup.add(_exitWrapperItem);
		popup.add(_startServiceItem);
		popup.add(_exitItem);
		popup.validate();

		ti.addMouseListener(new MouseListener()
		{

			public void mouseClicked(MouseEvent e)
			{
				popup.show(e.getComponent(), e.getX() - popup.getWidth(), e.getY() - popup.getHeight());

			}

			public void mouseEntered(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}

			public void mouseExited(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}

			public void mousePressed(MouseEvent e)
			{
				popup.show(e.getComponent(), e.getX() - popup.getWidth(), e.getY() - popup.getHeight());

			}

			public void mouseReleased(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}

		});

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				stop = true;
				synchronized (tray)
				{
					tray.remove(ti);
				}
			}
		});

		try
		{
			tray.add(ti);
		}
		catch (AWTException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(0);
		}

		init = true;

	}

	private InputStream getImage(String icon)
	{
		InputStream f = null;
		if (icon == null)
			f = findFile("/resources/console.png");
		else
		{
			f = findFile(icon);
			if (f == null)
			{
				try
				{
					System.out.println("System Tray: " + new File(icon).getCanonicalPath() + " not found -> default icon");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				f = findFile("/resources/console.png");
			}
		}
		if (f == null)
		{
			System.out.println("System Tray: no icon found -> abort");
			return null;
		}
		return f;
	}

	public Image getStateImage(int state)
	{
		switch (state)
		{
		case WrappedProcess.STATE_RUNNING:
			return iconRunning;
		case WrappedProcess.STATE_IDLE:
			return iconIdle;
		default:
			return iconElse;
		}
	}

	public String getStateToolTip(int state)
	{
		switch (state)
		{
		case WrappedProcess.STATE_RUNNING:
			return "Running";
		case WrappedProcess.STATE_IDLE:
			return "Idle";
		case WrappedProcess.STATE_RESTART:
		case WrappedProcess.STATE_RESTART_START:
		case WrappedProcess.STATE_RESTART_STOP:
		case WrappedProcess.STATE_RESTART_WAIT:
			return "Restarting";
		case WrappedProcess.STATE_STARTING:
			return "Starting";
		case WrappedProcess.STATE_USER_STOP:
		case WrappedProcess.STATE_STOP:
			return "Stopping";
		default:
			return "Other";
		}
	}

	synchronized public void showState(int state)
	{
		int oldState = _currentState;
		_currentState = state;
		String strState = getStateToolTip(state);
		if (oldState != _currentState)
			this.message("STATE CHANGED", getStateToolTip(oldState) + " -> " +getStateToolTip(_currentState));
		if (_console != null && _process != null)
		{
			_console.setState(strState);
			_console.setAppRestartCount(_process.getTotalRestartCount(), _process.getRestartCount());
			_console.setAppPid(_process.getAppPid());
			_console.setAppStarted(_process.getAppStarted());
			_console.setAppStopped(_process.getAppStopped());
			_console.setExitCode(_process.getExitCode());
			_console.setTimer(_process.isTimerActive());
			_console.setCondition(_process.isConditionActive());
		}

		Image image = getStateImage(state);
		if (image != currentImage)
		{
			ti.setImage(image);
			currentImage = image;
			currentToolTip = toolTipPrefix + strState;
			ti.setToolTip(currentToolTip);
		}
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	ImageIcon createImageIcon(String path)
	{
		Image image = createImage(path);
		if (image == null)
			return null;
		return new ImageIcon(image);
	}

	private Image createImage(String path)
	{
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null)
		{
			return Toolkit.getDefaultToolkit().getImage(imgURL);
		}
		else
		{
			if (new File(path).exists())
				return Toolkit.getDefaultToolkit().getImage(path);
			return null;
		}
	}

	private InputStream findFile(String path)
	{
		InputStream result = null;
		try
		{
			result = getClass().getResourceAsStream(path);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (result != null)
			return result;
		File f = null;
		if (result == null)
			f = new File(path);
		if (f.exists())
			try
			{
				result = new FileInputStream(f);
				return result;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		return null;

	}

	private Image createColorImage(InputStream imageFile, Color color, Dimension d) throws Exception
	{
		BufferedImage image = ImageIO.read(imageFile);
		imageFile.close();

		if (d != null)
		{
			BufferedImage bufferedResizedImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bufferedResizedImage.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.drawImage(image, 0, 0, d.width, d.height, null);
			g2d.dispose();
			image = bufferedResizedImage;

		}
		if (color != null)
		{
			Graphics g = image.getGraphics();
			int w = image.getWidth(null);
			int h = image.getHeight(null);
			int rw = w / 2;
			int rh = h / 2;
			Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), 200);
			g.setColor(c);
			g.fillRoundRect(0, h - rh, rw, rh, rw, rh);
		}

		return image;
	}

	public static void main(String[] args) throws InterruptedException
	{
		WrapperTrayIconImpl t = new WrapperTrayIconImpl("test", null);// "tomcat.gif");
		while (true)
		{
			Thread.sleep(2000);
			t.showState(WrappedProcess.STATE_RUNNING);
			Thread.sleep(2000);
			t.showState(WrappedProcess.STATE_IDLE);
			Thread.sleep(2000);
			t.showState(WrappedProcess.STATE_RESTART);
		}
	}

	public boolean isInit()
	{
		return init;
	}

	public void openConsole()
	{
		if (_console != null)
			return;
		_console = new Console(this);
		this.showState(_currentState);
		_console.setWrapperPid(_process.getWrapperPid());
		_console.setWrapperStarted(_process.getWrapperStarted());
		_console.setWrapperType(_process.getType());
	}

	public void closeConsole()
	{
		if (_console == null)
			return;
		_console.close();
		_console = null;
	}

	public void error(String caption, String message)
	{
		ti.displayMessage(toolTipPrefix + caption, message, TrayIcon.MessageType.ERROR);
	}

	public void info(String caption, String message)
	{
		ti.displayMessage(toolTipPrefix + caption, message, TrayIcon.MessageType.INFO);
	}

	public void message(String caption, String message)
	{
		ti.displayMessage(toolTipPrefix + caption, message, TrayIcon.MessageType.NONE);
	}

	public void warning(String caption, String message)
	{
		ti.displayMessage(toolTipPrefix + caption, message, TrayIcon.MessageType.WARNING);
	}

	public void setProcess(AbstractWrappedProcessMBean proxy)
	{
		_process = proxy;
		if (_process == null)
		{
			Image image = iconOffline;
			if (image != currentImage)
			{
				ti.setImage(image);
				currentImage = image;
				currentToolTip = toolTipPrefix + "OFFLINE";
				ti.setToolTip(currentToolTip);
			}

		}
	}

	public boolean isStop()
	{
		return stop;
	}
	

}
