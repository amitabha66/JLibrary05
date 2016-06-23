package org.rzo.yajsw.tray;

import org.rzo.yajsw.wrapper.AbstractWrappedProcessMBean;

public interface WrapperTrayIcon
{
	public boolean isInit();

	public void info(String caption, String message);

	public void error(String caption, String message);

	public void warning(String caption, String message);

	public void message(String caption, String message);

	public void setProcess(AbstractWrappedProcessMBean proxy);

	public void closeConsole();

	public void showState(int state);

	public boolean isStop();

}
