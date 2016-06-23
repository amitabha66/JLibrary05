package org.rzo.yajsw.wrapper;

import org.apache.commons.configuration.Configuration;
import org.rzo.yajsw.controller.AbstractController.ControllerListener;
import org.rzo.yajsw.controller.runtime.RuntimeController;
import org.rzo.yajsw.os.OperatingSystem;

public class WrappedRuntimeProcess extends AbstractWrappedProcess
{

	@Override
	void configProcess()
	{
		super.configProcess();
		_osProcess.setCommand(_config.getString("wrapper.image"));
		_osProcess.setPipeStreams(true, false);
	}

	@Override
	void postStart()
	{
	}

	@Override
	void stopController(int timeout)
	{
		_controller.stop(RuntimeController.STATE_USER_STOPPED);
		_osProcess.stop(timeout, 999);
	}

	public boolean reconnect(int pid)
	{
		try
		{
		_osProcess = OperatingSystem.instance().processManagerInstance().getProcess(pid);
		_osProcess.stop(10, 0);
		//this.start();
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
		}
		return true;
	}

	public void init()
	{
		super.init();
		if (_controller == null)
		{
			_controller = new RuntimeController(this);
			//configController();
		}
	}

	void configController()
	{
		
		ControllerListener listenerStopped = new ControllerListener()
		{
			public void fire()
			{
				System.out.println("listener stopped");
				if (_osProcess.isRunning())
					stop();
				if (allowRestart() && exitCodeRestart())
				{
					restartInternal();
				}
				else
				{
					setState(STATE_IDLE);
					if (_debug)
					{
						getWrapperLogger().info("giving up after " + _restartCount + " retries");
					}
				}

			}

		};
		_controller.addListener(RuntimeController.STATE_STOPPED, listenerStopped);
		

	}

	public String getType()
	{
		return "Native-" + super.getType();
	}

	public static void main(String[] args)
	{
		WrappedRuntimeProcess p = new WrappedRuntimeProcess();
		Configuration c = p.getLocalConfiguration();
		c.setProperty("wrapper.image", "notepad");// "test.bat");//notepad");//"c:/temp/test.bat");//
		c.setProperty("wrapper.working.dir", "c:/");
		p.init();
		p.start();
		p.waitFor(10000);
		System.out.println("stopping");
		p.stop();
		System.out.println("stopped " + p.getExitCode());
	}

}
