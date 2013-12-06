package de.mpc.pia.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpc.pia.webgui.compiler.CompilationThread;

@ManagedBean(eager=true,
	name="piaConfiguration")
@ApplicationScoped
public class PIAConfigurationProperties {
	
	/** the actual properties */
	private Properties properties;

	
	/** logger for this class */
	private static final Logger logger = Logger.getLogger(CompilationThread.class);
	
	
	public PIAConfigurationProperties() {
		logger.info("building PIAConfigurationProperties");
		
		// Get the inputStream
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		
		InputStream inputStream = externalContext.getResourceAsStream("/config/pia.properties");
		
		properties = new Properties();
		
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("Error reading the properties file! " + e);
			}
		}
	}
	
	
	/**
	 * Get a configuration value from the pia.properties file.
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public String getPIAProperty(String key, String defaultValue) {  
		return properties.getProperty(key, defaultValue);
	}
}
