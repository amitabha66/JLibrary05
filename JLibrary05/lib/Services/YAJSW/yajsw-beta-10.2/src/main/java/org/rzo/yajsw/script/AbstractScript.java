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
package org.rzo.yajsw.script;

import org.rzo.yajsw.wrapper.WrappedProcess;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractScript.
 */
public abstract class AbstractScript implements Script
{

	/** The _name. */
	String			_name;

	/** The _timeout. */
	int				_timeout	= 30000;

	WrappedProcess	_process;

	String			_id;

	String[]		_args;

	/**
	 * Instantiates a new abstract script.
	 * 
	 * @param script
	 *            the script
	 */
	public AbstractScript(String script, String id, WrappedProcess process, String[] args)
	{
		_name = script;
		_process = process;
		_id = id;
		_args = args;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.script.Script#execute(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.Object)
	 */
	public abstract Object execute();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.script.Script#getScript()
	 */
	public String getScript()
	{
		return _name;
	}

	/**
	 * Gets the timeout.
	 * 
	 * @return the timeout
	 */
	public int getTimeout()
	{
		return _timeout;
	}

	/**
	 * Sets the timeout.
	 * 
	 * @param timeout
	 *            the new timeout
	 */
	public void setTimeout(int timeout)
	{
		_timeout = timeout;
	}
	
	// TODO clean this up
	public void execute(String data)
	{
		execute();
	}

}
