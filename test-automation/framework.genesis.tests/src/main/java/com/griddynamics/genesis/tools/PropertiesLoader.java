package com.griddynamics.genesis.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Class is designed to read values from property files used in test framework
 * 
 * @author mlykosova
 *
 */
public class PropertiesLoader {

	public PropertiesLoader() {

	}

	public Properties loadPropertiesFromFile(String fileName) {
		Properties props = new Properties();
		String filePath = System.getProperty(fileName + ".path") != null ? System
				.getProperty(fileName + ".path") : getClass().getResource("/" + fileName + ".properties").getPath();
		FileInputStream fis;
		try {
			fis = new FileInputStream(filePath);
			props.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return props;
	}
}
