package org.rzo.netty.ahessian.rpc.client;

import org.rzo.netty.ahessian.rpc.message.HessianRPCReplyMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Future object returned when executing a remote method invocation.
 * <br>
 * Note: within a continuation scenario, get() will return the last result received.
 * Results sent by the server will override the result available on the client.
 */
public class HessianProxyFuture implements Future<Object>
{
	
	/** indicates if the invocation has been completed */
	private boolean					_done			= false;
	
	/** TODO indicates if the invocation has been canceled or timed out. */
	private boolean					_canceled		= false;
	
	/** result of the invocation. */
	private HessianRPCReplyMessage	_result			= null;

	private Lock			_lock			= new ReentrantLock();
	private Condition		_resultReceived	= _lock.newCondition();
	private Collection<Runnable> _listeners = Collections.synchronizedCollection(new ArrayList<Runnable>());

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		_canceled = true;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	
	public Object get() throws InterruptedException, ExecutionException
	{
		Object result = null;
		_lock.lock();
		try
		{
			while (_result == null)
			{
				_resultReceived.await();
			}
			if (_result.getFault() != null)
				throw new ExecutionException(_result.getFault());
			else
				return _result.getValue();
		}
		finally
		{
			_lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		Object result = null;
		_lock.lock();
		try
		{
			if (_result == null)
				_resultReceived.await(timeout, unit);
			if (_result == null)
				throw new TimeoutException();
			if (_result.getFault() != null)
				throw new ExecutionException(_result.getFault());
			else
				return _result.getValue();
		}
		finally
		{
			_lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	
	public boolean isCancelled()
	{
		return _canceled;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isDone()
	 */
	
	public boolean isDone()
	{
		return _done;
	}

	protected void set(HessianRPCReplyMessage message)
	{
		_lock.lock();
		try
		{
			_done = true;
			_result = message;
			_resultReceived.signal();
			callListners();
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	private void callListners()
	{
		synchronized(_listeners)
		{
			for (Runnable listener : _listeners)
				listener.run();
		}
	}

	/**
	 * Adds the listener.
	 * 
	 * @param listener the listener
	 */
	public void addListener(Runnable listener)
	{
		_lock.lock();
		if (isDone())
			listener.run();
		else
			_listeners.add(listener);
		_lock.unlock();
	}

	/**
	 * Removes the listener.
	 * 
	 * @param listener the listener
	 */
	public void removeListener(Runnable listener)
	{
		_listeners.remove(listener);
	}

}
