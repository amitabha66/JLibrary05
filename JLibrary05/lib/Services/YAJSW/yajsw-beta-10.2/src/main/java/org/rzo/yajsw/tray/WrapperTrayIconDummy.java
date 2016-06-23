package org.rzo.yajsw.tray;

import org.rzo.yajsw.wrapper.AbstractWrappedProcessMBean;

public class WrapperTrayIconDummy implements WrapperTrayIcon
{

	public boolean isInit()
	{
		return true;
	}

	public void error(String caption, String message)
	{
		// TODO Auto-generated method stub

	}

	public void info(String caption, String message)
	{
		// TODO Auto-generated method stub

	}

	public void message(String caption, String message)
	{
		// TODO Auto-generated method stub

	}

	public void warning(String caption, String message)
	{
		// TODO Auto-generated method stub

	}

	public void setProcess(AbstractWrappedProcessMBean proxy)
	{
		// TODO Auto-generated method stub
		
	}

	public void closeConsole()
	{
		// TODO Auto-generated method stub
		
	}

	public void showState(int state)
	{
		// TODO Auto-generated method stub
		
	}

	public boolean isStop()
	{
		// TODO Auto-generated method stub
		return false;
	}

}
