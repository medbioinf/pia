package de.mpc.pia.modeller.report.settings;



public class Setting {
	/** the human readable name of this setting */
	private String name;
	
	/** the machine readable unique name of this setting */
	private String shortName;
	
	/** the (unprocessed) value of this setting */
	private String value;
	
	/** the (unprocessed) default value of this setting */
	private String defaultValue;
	
	/** the type of this setting */
	private SettingType type;
	
	/** additional params for this setting */
	private Object params;
	
	
	
	/**
	 * Basic constructor for the setting.
	 * 
	 * @param shortName	the unique machine readable name
	 * @param name the human readable name
	 * @param defaultValue the default value
	 */
	public Setting(String shortName, String name, String defaultValue,
			String typeName, Object params) {
		this.name = name;
		this.shortName = shortName;
		this.defaultValue = defaultValue;
		this.value = null;
		this.type = SettingType.getTypeByName(typeName);
		this.params = params;
	}
	
	
	/**
	 * Getter for the human readable name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Getter for the unique machine readable name.
	 * 
	 * @return
	 */
	public String getShortName() {
		return shortName;
	}
	
	
	/**
	 * Getter for the type.
	 * 
	 * @return
	 */
	public SettingType getType() {
		return type;
	}
	
	
	/**
	 * Getter for the value.
	 * 
	 * @return
	 */
	public String getValue() {
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	
	/**
	 * Setter for the value.
	 * 
	 * @return
	 */
	public void setValue(String value) {
		this.value = value;
	}
	

	/**
	 * Updates the params to the new Object.
	 * 
	 * @param params
	 */
	public void updateParams(Object params) {
		this.params = params;
	}
	
	
	/**
	 * Returns the type processed params for this setting.
	 * 
	 * @return
	 */
	public Object getTypedParams() {
		return type.getTypedParams(params);
	}
}
