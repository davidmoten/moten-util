package au.edu.anu.delibdem.qsort.gui;

import java.io.IOException;
import java.util.Properties;

public class ConfigurationFromProperties implements Configuration {

	private static final String CONFIGURATION_RESOURCE = "/configuration.properties";
	private final Properties props;

	public ConfigurationFromProperties() {
		props = new Properties();
		try {
			props.load(getClass().getResourceAsStream(CONFIGURATION_RESOURCE));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// override with System properties
		props.putAll(System.getProperties());
	}

	@Override
	public boolean provideDataSelectionForEveryVariable() {
		return getBoolean("provide.data.selection.for.every.variable");
	}

	private boolean getBoolean(String name) {
		if (props.get(name) == null)
			throw new RuntimeException("property " + name
					+ " not found in resource /configuration.properties"
					+ CONFIGURATION_RESOURCE);
		return "true".equalsIgnoreCase(props.get(name).toString().trim());
	}

}