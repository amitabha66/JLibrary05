package org.rzo.yajsw.tray;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.text.Element;

import org.rzo.yajsw.util.DaemonThreadFactory;

public class Console extends JFrame
{
	WrapperTrayIconImpl				_trayIcon;
	boolean							stop;
	protected static final Executor	executor		= Executors.newCachedThreadPool(new DaemonThreadFactory("console"));
	int								maxLines		= 500;
	ConsoleForm						_consoleForm	= new ConsoleForm();
	OutputStream					_currentOutputStream;
	SimpleDateFormat				_dateTimeFormat	= new SimpleDateFormat();
	ByteFormat						_byteFormat		= new ByteFormat();
	Icon							_okIcon;

	public Console(WrapperTrayIconImpl trayIcon)
	{
		_trayIcon = trayIcon;
		this.setTitle(_trayIcon.toolTipPrefix + "Console");
		this.addWindowListener(new WindowEventHandler());
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		_okIcon = _trayIcon.createImageIcon("/resources/tick.png");
		initOutput();
		initInput();
		initPerformance();
		initButtons();

		this.getContentPane().add(_consoleForm);

		this.pack();
		this.setVisible(true);
		stop = false;
	}

	private void initButtons()
	{
		initButton(_consoleForm._EXIT_TRAY_ICON_BUTTON, _trayIcon._exitItem);
		initButton(_consoleForm._EXIT_WRAPPER_BUTTON, _trayIcon._exitWrapperItem);
		initButton(_consoleForm._THREAD_DUMP_WRAPPER_BUTTON, _trayIcon._threadDumpWrapperItem);
		initButton(_consoleForm._RESTART_BUTTON, _trayIcon._restartItem);
		initButton(_consoleForm._START_BUTTON, _trayIcon._startItem);
		initButton(_consoleForm._STOP_BUTTON, _trayIcon._stopItem);
		initButton(_consoleForm._STOP_TIMER_BUTTON, _trayIcon._stopTimerItem);
		initButton(_consoleForm._THREAD_DUMP_BUTTON, _trayIcon._threadDumpItem);
		initButton(_consoleForm._jbutton1, _trayIcon._closeConsoleItem);
	}

	private void initButton(JButton button, JMenuItem item)
	{
		button.setAction(item.getAction());
	}

	private void initPerformance()
	{
		executor.execute(new Runnable()
		{

			public void run()
			{
				while (!stop)
				{
					setAppCpu(_trayIcon._process.getAppCpu());
					setAppHandles(_trayIcon._process.getAppHandles());
					setAppMemory(_trayIcon._process.getAppMemory());
					setAppThreads(_trayIcon._process.getAppThreads());
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
		});
	}

	 void setState(String state)
	{
		if (state != null)
			_consoleForm._state.setText(state);
		_consoleForm._state.repaint();
	}

	 void setAppPid(int pid)
	{
		if (pid > 0)
			_consoleForm._appPid.setText("" + pid);
		else
			_consoleForm._appPid.setText("-");

	}

	 void setWrapperPid(int pid)
	{
		if (pid > 0)
			_consoleForm._wPid.setText("" + pid);
		else
			_consoleForm._wPid.setText("-");
	}

	 void setTrigger(String trigger)
	{
		if (trigger != null)
			_consoleForm._trigger.setText(trigger);
	}

	 void setAppStarted(Date time)
	{
		if (time != null)
			_consoleForm._appStartTime.setText(_dateTimeFormat.format(time));
	}

	 void setAppStopped(Date time)
	{
		if (time != null)
			_consoleForm._appStopTime.setText(_dateTimeFormat.format(time));
	}

	 void setAppRestartCount(int total, int count)
	{
		if (count > 0)
			_consoleForm._count.setText(total+"[" + count + "]");
	}

	 void setWrapperStarted(Date time)
	{
		if (time != null)
			_consoleForm._wStartTime.setText(_dateTimeFormat.format(time));
	}

	 void setAppThreads(int count)
	{
		if (count > 0)
			_consoleForm._threads.setText("" + count);
		else
			_consoleForm._threads.setText("-");

	}

	 void setAppHandles(int count)
	{
		if (count > 0)
			_consoleForm._handles.setText("" + count);
		else
			_consoleForm._handles.setText("-");
	}

	 void setAppMemory(long bytes)
	{
		if (bytes > 0)
			_consoleForm._memory.setText(_byteFormat.format(bytes));
		else
			_consoleForm._memory.setText("-");
	}

	 void setAppCpu(int count)
	{
		if (count >= 0)
			_consoleForm._cpu.setText("" + count);
		else
			_consoleForm._cpu.setText("-");

	}

	 void setExitCode(int code)
	{
		if (code >= 0)
			_consoleForm._exitCode.setText("" + code);
	}

	 void setWrapperType(String type)
	{
		_consoleForm._wrapperType.setText(type);
	}

	 void setCondition(boolean active)
	{
		if (active)
		{
			_consoleForm._condition.setText("");
			_consoleForm._condition.setIcon(_okIcon);
		}
		else
		{
			_consoleForm._condition.setText("-");
			_consoleForm._condition.setIcon(null);
		}

	}

	void setTimer(boolean active)
	{
		if (active)
		{
			_consoleForm._timer.setText("");
			_consoleForm._timer.setIcon(_okIcon);
		}
		else
		{
			_consoleForm._timer.setText("-");
			_consoleForm._timer.setIcon(null);
		}

	}

	private void initInput()
	{
		
		_consoleForm._input.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// process may have not yet been started or it may have been
				// stopped
				try
				{
				if (_trayIcon._process == null || !_trayIcon._process.hasOutput())
				{
					_consoleForm._input.setText("No input possible");
					_consoleForm._input.selectAll();
					return;
				}
				String txt = _consoleForm._input.getText();
				_trayIcon._process.setOutput(txt);
				_consoleForm._input.selectAll();
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
			}
			}
		});
		
	}

	private void initOutput()
	{
		
		executor.execute(new Runnable()
		{

			public void run()
			{
				_trayIcon._process.startDrain();
				while (!stop)
				{
					_trayIcon.showState(_trayIcon._process.getState());
					String line;
					while ((line = _trayIcon._process.readDrainLine()) != null)
						addLine(line);
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
				_trayIcon._process.stopDrain();

			}

		});
		
	}

	private void addLine(String line)
	{
		JTextArea textArea = _consoleForm._output;
		textArea.append(line + "\n");
		if (textArea.getLineCount() > maxLines)
		{
			Element root = textArea.getDocument().getDefaultRootElement();
			Element firstLine = root.getElement(0);

			try
			{
				textArea.getDocument().remove(0, firstLine.getEndOffset());
			}
			catch (Exception ble)
			{
				System.out.println(ble.getMessage());
			}
		}
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public void close()
	{
		stop = true;
		this.setVisible(false);
		this.dispose();
	}

	class WindowEventHandler extends WindowAdapter
	{
		public void windowClosing(WindowEvent evt)
		{
			close();
			_trayIcon.closeConsole();
		}
	}

}
