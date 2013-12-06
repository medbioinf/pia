package de.mpc.pia.modeller.report.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

public enum SettingType {
	/**
	 * The setting uses a selectOneRadio HTML element for settings.
	 * 
	 * params should be: Map<String, String>
	 */
	SELECT_ONE_RADIO {
		@Override
		public String getShortName() {
			return "select_one_radio";
		}
		
		@Override
		public List<SelectItem> getTypedParams(Object params) {
			List<SelectItem> items = new ArrayList<SelectItem>();
			
			if (params instanceof Map<?, ?>) {
				for (Map.Entry<?, ?> paramIt : ((Map<?, ?>) params).entrySet()) {
					items.add( new SelectItem(paramIt.getKey().toString(), paramIt.getValue().toString()) );
				}
			} else {
				// TODO: nice exception
				System.err.println("SEVERE: params are not in the format Map<?, ?> for SELECT_ONE_RADIO");
			}
			
			return items;
		}
	},
	
	/**
	 * The setting is a simple text input.
	 */
	INPUT {
		@Override
		public String getShortName() {
			return "input";
		}

		@Override
		public Object getTypedParams(Object params) {
			// does not need any params (yet)
			return null;
		}
	},
	
	/**
	 * No setting, but a simple message.
	 */
	MESSAGE {
		@Override
		public String getShortName() {
			return "message";
		}

		@Override
		public Object getTypedParams(Object params) {
			// does not need any params (yet)
			return null;
		}
	},
	;
	
	
	/**
	 * Returns the short name of this SettingType.
	 * @return
	 */
	public abstract String getShortName();
	
	
	/**
	 * Returns the processed params for this type.
	 * @return
	 */
	public abstract Object getTypedParams(Object params);
	
	
	/**
	 * Returns the SettingType with the given typeName or null, if none
	 * was found with this typeName.
	 * 
	 * @return
	 */
	public static SettingType getTypeByName(String typeName) {
		for (SettingType type : SettingType.values()) {
			if (type.getShortName().equals(typeName)) {
				return type;
			}
		}
		
		return null;
	}
}