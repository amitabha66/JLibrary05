package org.rzo.netty.ahessian.example.chat.client;

public interface AsynchChatService
{
	public Object logon(String user);
	public Object putMessage(String message);
	public Object getMessage();
	public Object logoff();
}
