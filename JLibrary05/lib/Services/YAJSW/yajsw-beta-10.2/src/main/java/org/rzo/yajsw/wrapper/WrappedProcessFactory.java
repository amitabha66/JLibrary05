package org.rzo.yajsw.wrapper;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.rzo.yajsw.config.YajswConfiguration;
import org.rzo.yajsw.config.YajswConfigurationImpl;

public class WrappedProcessFactory
{
	public static WrappedProcess createProcess(YajswConfiguration config)
	{
		if (config.getString("wrapper.image") != null)
			return new WrappedRuntimeProcess();
		else if (config.getString("wrapper.groovy") != null)
			return new WrappedGroovyProcess();
		return new WrappedJavaProcess();
	}
	
	public static Object createProcess(Map map)
	{
		Configuration localConf = new MapConfiguration(map);
		YajswConfiguration conf = new YajswConfigurationImpl(localConf, true);
		WrappedProcess process = createProcess(conf);
		process.setLocalConfiguration(localConf);
		process.setUseSystemProperties(false);
		process.init();
		return process;
	}

}
