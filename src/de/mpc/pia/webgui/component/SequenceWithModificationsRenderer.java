package de.mpc.pia.webgui.component;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

import org.richfaces.TooltipLayout;
import org.richfaces.TooltipMode;
import org.richfaces.component.UIPanel;
import org.richfaces.component.UITooltip;
import org.richfaces.renderkit.html.PanelRenderer;

import de.mpc.pia.intermediate.Modification;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


@FacesRenderer(componentFamily = "org.richfaces.Panel", rendererType = "de.mpc.pia.webgui.component.SequenceWithModifications")
public class SequenceWithModificationsRenderer extends PanelRenderer {
	
	@Override
	public void renderChildren(FacesContext context, UIComponent component) throws IOException {
		if (component instanceof SequenceWithModifications) {
			SequenceWithModifications specComponent = (SequenceWithModifications)component;
			
			String sequence = specComponent.getSequence();
			Map<Integer, Modification> modifications = specComponent.getModifications();
			
			if (sequence != null) {
				renderSpectrumSequence(context, sequence, modifications);
			}
		}
	}
	
	
	/**
	 * Renders the spectrum sequence into the given panel, using tooltips for
	 * modification highlighting.
	 * 
	 * @param context
	 * @param panel
	 * @param spectrum
	 */
	public void renderSpectrumSequence(FacesContext context, String sequence, Map<Integer, Modification> modifications)
		throws IOException {
		Application app = context.getApplication();
		ResponseWriter writer = context.getResponseWriter();
		
		// put the modifications into a tree map, so that we have them ordered
		TreeMap<Integer, Modification> mods = new TreeMap<Integer, Modification>();
		
		if (modifications != null) {
			mods.putAll(modifications);
		}
		
		int pos = 0;
		int lastPos = 0;
		String residue;
		String modDesc;
		UIPanel modificationPanel;
		UITooltip modificationTip;
		HtmlOutputText text;
		
		PanelRenderer renderer = new PanelRenderer();
		
		for (Map.Entry<Integer, Modification> modIt : mods.entrySet()) {
			pos = modIt.getKey();
			
			// first add the unmodified residues from last to here
			if (pos - lastPos > 1) {
				writer.write(sequence, lastPos, pos-lastPos-1);
			}
			
			if ((pos == 0) ||
					(pos == sequence.length()+1)) {
				// special case: terminal modification
				residue = ".";
			} else {
				residue = sequence.substring(pos-1, pos);
			}
			
			modificationPanel = (UIPanel)app.createComponent(context, UIPanel.COMPONENT_TYPE, "org.richfaces.PanelRenderer");
			modificationPanel.setStyleClass("modification-panel-tooltip");
			modificationPanel.setBodyClass("modification-panel-tooltip-body");
			
			text = (HtmlOutputText)app.createComponent(HtmlOutputText.COMPONENT_TYPE);
			text.setValue(residue);
			modificationPanel.getChildren().add(text);
			
			modificationTip = (UITooltip)app.createComponent(context, UITooltip.COMPONENT_TYPE, "org.richfaces.TooltipRenderer");;
			modificationTip.setLayout(TooltipLayout.block);
			modificationTip.setMode(TooltipMode.client);
			modificationTip.setFollowMouse(false);
			modificationTip.setStyleClass("modification-tooltip");
			
			text = (HtmlOutputText)app.createComponent(HtmlOutputText.COMPONENT_TYPE);
			
			if (modIt.getValue().getDescription() != null) {
				modDesc = modIt.getValue().getMass()+": "+modIt.getValue().getDescription();
			} else {
				modDesc = Double.toString(modIt.getValue().getMass());
			}
			text.setValue(modDesc);
			modificationTip.getChildren().add(text);
			
			modificationPanel.getChildren().add(modificationTip);
			
			// render the modification panel
			renderer.encodeBegin(context, modificationPanel);
			renderer.encodeEnd(context, modificationPanel);
			
			lastPos = pos;
		}
		
		// add the remaining residues
		if (lastPos <= sequence.length()) {
			writer.write(sequence, lastPos, sequence.length()-lastPos);
		}
	}
}
