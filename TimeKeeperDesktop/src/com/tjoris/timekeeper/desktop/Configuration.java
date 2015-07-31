package com.tjoris.timekeeper.desktop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration
{
	private static final String kSPACE_SYSTEM = "System.";
	private static final String kSPACE_USER = "User.";
	
	private final File fPropertyFile;
	private final Properties fProperties;
	
	public Configuration(final File propertyFile) throws IOException
	{
		fPropertyFile = propertyFile;
		fProperties = new Properties();
		if (fPropertyFile.exists())
		{
			final BufferedInputStream propertyInputStream = new BufferedInputStream(new FileInputStream(fPropertyFile));
			fProperties.load(propertyInputStream);
			propertyInputStream.close();
		}
	}
	
	public String getSystemProperty(final String key)
	{
		return fProperties.getProperty(kSPACE_SYSTEM + key);
	}
	
	public void setSystemProperty(final String key, final String value)
	{
		fProperties.setProperty(kSPACE_SYSTEM + key, value);
	}
	
	public String getUserProperty(final String key)
	{
		return fProperties.getProperty(kSPACE_USER + key);
	}
	
	public void setUserProperty(final String key, final String value)
	{
		fProperties.setProperty(kSPACE_USER + key, value);
	}
	
	public void clear()
	{
		fProperties.clear();
	}
	
	public void saveProperties() throws IOException
	{
		fPropertyFile.getParentFile().mkdirs();
		final BufferedOutputStream propertyOutputStream = new BufferedOutputStream(new FileOutputStream(fPropertyFile));
		fProperties.store(propertyOutputStream, "TimeKeeper");
		propertyOutputStream.close();
	}
}
