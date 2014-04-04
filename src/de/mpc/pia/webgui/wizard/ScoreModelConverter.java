package de.mpc.pia.webgui.wizard;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

@FacesConverter("ScoreModelConverter")
public class ScoreModelConverter implements Converter {
	public Object getAsObject(FacesContext facesContext, UIComponent component,
			String s) {
		ScoreModelEnum modelType =
				ScoreModelEnum.getModelByDescription(s);
		ScoreModel model;
		
		if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
			model = new ScoreModel(0.0, s, s);
		} else {
			model = new ScoreModel(0.0, modelType);
		}
		
		System.err.println("getAsObject: " + model);
		
		return model;
	}
	
	
	public String getAsString(FacesContext facesContext, UIComponent component,
			Object o) {
		
		System.err.println("getAsString: " + o);
		
		if (o == null) {
			return null;
		}
		
		return ((ScoreModel) o).getShortName();
	}
}
