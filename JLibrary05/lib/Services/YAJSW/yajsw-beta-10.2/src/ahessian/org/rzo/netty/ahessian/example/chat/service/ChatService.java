package org.rzo.netty.ahessian.example.chat.service;

public interface ChatService
{
	public void logon(String user);
	public void putMessage(String message);
	public String getMessage();
	public void logoff();
}
