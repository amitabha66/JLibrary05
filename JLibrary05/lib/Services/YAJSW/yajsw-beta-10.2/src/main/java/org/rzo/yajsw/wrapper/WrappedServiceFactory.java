package org.rzo.yajsw.wrapper;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.rzo.yajsw.config.YajswConfiguration;
import org.rzo.yajsw.config.YajswConfigurationImpl;

public class WrappedServiceFactory
{
	public static Object createService(Map map)
	{
		Configuration localConf = new MapConfiguration(map);
		WrappedService service = new WrappedService();
		service.setLocalConfiguration(localConf);
		service.init();
		return service;
	}

}
