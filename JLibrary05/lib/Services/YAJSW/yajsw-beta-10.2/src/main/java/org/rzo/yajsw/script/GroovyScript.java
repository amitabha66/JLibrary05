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

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.vfs.FileObject;
import org.codehaus.groovy.control.CompilationFailedException;
import org.rzo.yajsw.boot.WrapperLoader;
import org.rzo.yajsw.util.VFSUtils;
import org.rzo.yajsw.wrapper.WrappedJavaProcess;
import org.rzo.yajsw.wrapper.WrappedProcess;


// TODO: Auto-generated Javadoc
/**
 * The Class GroovyScript.
 */
public class GroovyScript extends AbstractScript
{

	static Map		context	= Collections.synchronizedMap(new HashMap());
	/** The binding. */
	Binding			binding;

	WrappedProcess	process;
	
	Logger _logger;

	GroovyObject	_script;

	/**
	 * Instantiates a new groovy script.
	 * 
	 * @param script
	 *            the script
	 * @throws IOException
	 * @throws CompilationFailedException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public GroovyScript(String script, String id, WrappedProcess process, String[] args) throws CompilationFailedException, IOException,
			InstantiationException, IllegalAccessException
	{
		super(script, id, process, args);
		//System.out.println("groovy script "+new File(".").getCanonicalPath());
		ClassLoader parent = getClass().getClassLoader();
		GroovyClassLoader loader = new GroovyClassLoader(parent);
		setGroovyClasspath(loader);
		FileObject fileObject = VFSUtils.resolveFile(".", script);
		InputStream in = fileObject.getContent().getInputStream();
		Class groovyClass = loader.parseClass(in);
		in.close();

		// let's call some method on an instance
		_script = (GroovyObject) groovyClass.newInstance();
		binding = (Binding) _script.invokeMethod("getBinding", null);
		binding.setVariable("args", args);
		binding.setVariable("callCount", 0);
		binding.setVariable("context", context);
		if (process != null)
		_logger = process.getWrapperLogger();
		binding.setVariable("logger", _logger);
	}

	private void setGroovyClasspath(GroovyClassLoader loader)
	{
		ArrayList cp = WrapperLoader.getGroovyClasspath();
		for (Iterator it = cp.listIterator(); it.hasNext(); )
			loader.addURL((URL)it.next());
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rzo.yajsw.script.AbstractScript#execute(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.Object)
	 */
	public Object execute()
	{
		Object result = null;
		
		if (_script == null)
		{
			System.out.println("cannot execute script " + _name);
			return null;
		}
		binding.setVariable("id", _id);
		if (_process != null)
		{
		binding.setVariable("state", _process.getStringState());
		binding.setVariable("count", _process.getRestartCount());
		binding.setVariable("pid", _process.getAppPid());
		binding.setVariable("exitCode", _process.getExitCode());
		binding.setVariable("line", _process.getTriggerLine());
		binding.setVariable("process", _process);
		}
		try
		{
			result = _script.invokeMethod("run", new Object[]
			{});

		}
		catch (Throwable e)
		{
			if (_logger != null)
			_logger.log(Level.INFO, "execption in script "+this._name, e);
			else
				e.printStackTrace();
		}
		binding.setVariable("callCount", ((Integer) binding.getVariable("callCount")).intValue() + 1);
		return result;
	}

	public static void main(String[] args) throws Exception, IOException, InstantiationException, IllegalAccessException
	{
		WrappedJavaProcess w = new WrappedJavaProcess();
		w.getLocalConfiguration().setProperty("wrapper.config", "conf/wrapper.helloworld.conf");
		w.init();
		GroovyScript script = new GroovyScript("./scripts/timeCondition.gv", "id", w, new String[]
		{ "11", "12" });
		script.execute();
		script.execute();
		script = new GroovyScript("./scripts/fileCondition.gv", "id", w, new String[]
		{ "anchor.lck" });
		script.execute();
		script.execute();
		script = new GroovyScript("./scripts/snmpTrap.gv", "id", w, new String[]
		{ "192.168.0.1", "1", "msg" });
		script.execute();

	}

}
