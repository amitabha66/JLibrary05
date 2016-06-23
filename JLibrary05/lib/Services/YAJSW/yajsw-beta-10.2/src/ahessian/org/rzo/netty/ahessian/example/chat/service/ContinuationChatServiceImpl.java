package org.rzo.netty.ahessian.example.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.rzo.netty.ahessian.rpc.server.Continuation;
import org.rzo.netty.ahessian.session.Session;

public class ContinuationChatServiceImpl
{
	static List<Continuation> _continuations = Collections.synchronizedList(new ArrayList());
	
	public void logon(Continuation continuation, String user)
	{
		if (user == null || user.length() == 0)
			throw new RuntimeException("user null");
		String currentUser = (String) continuation.getSession().getAttribute("user");
		if (currentUser == null)
		{
			// first time -> attach user to session
			continuation.getSession().setAttribute("user", user);
			// tell others that we have a new user
			putMessage(continuation, new Date()+":"+user+": user logged in");
		}
		// already logged on -> check if session matches user
		else if (!currentUser.equals(user))
			throw new RuntimeException("wrong user");
		// inform the client that we are done.
		continuation.complete(null);
	}
	
	public void putMessage(Continuation continuation, String message)
	{

		String currentUser = (String) continuation.getSession().getAttribute("user");
		if (currentUser == null)
			throw new RuntimeException("missing user");
		String displayMessage = new Date()+":"+currentUser + ":"+message;
		
		synchronized(_continuations)
		{
			for (Continuation toDo : _continuations)
			{
				toDo.send(displayMessage);
			}
				
		}
		// nothing else. if the client who sent the message did not call getMessage
		// he will not see the message
		
	}
	
	public void getMessage(Continuation continuation)
	{
		Session session = continuation.getSession();
		synchronized(_continuations)
		{
			for (Continuation toDo : _continuations)
			{
				// if the session already sent a getMessage continuation -> ignore
				if (toDo.getSession() == session)
					return;
			}
			// remember the request
			_continuations.add(continuation);				
		}		
	}
	
	public void logoff()
	{
		//TODO
	}


}
