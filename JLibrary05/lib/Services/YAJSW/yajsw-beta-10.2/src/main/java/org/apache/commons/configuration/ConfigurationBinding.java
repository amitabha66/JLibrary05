package org.apache.commons.configuration;

import java.util.Map;

import org.apache.commons.configuration.Configuration;

import groovy.lang.Binding;

public class ConfigurationBinding extends Binding
{
	Configuration _conf;
	Map _utils;
	public ConfigurationBinding(Configuration conf, Map utils)
	{
		_conf = conf;
		_utils = utils;
	}
	
	public Object getVariable(String name)
	{
		Object result = null;
		if (_utils != null)
			result = _utils.get(name);
		if (result == null)
			result = _conf.getProperty(name);
		if (result == null)
			result = super.getVariable(name);
		return result;
	}

	public Object getProperty(String name)
	{
		return getVariable(name);
	}

}
