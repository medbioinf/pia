package de.mpc.pia.webgui.component;

import javax.faces.view.facelets.*;

public class SequenceWithModificationsTagHandler extends ComponentHandler {
	
    private static final String ATTR_SEQUENCE = "sequence";
    
    private static final String ATTR_MODIFICATIONS = "modifications";
    
    private TagAttribute sequence;
    
    private TagAttribute modifications;
    
    public SequenceWithModificationsTagHandler(ComponentConfig config) {
        super(config);
        this.sequence = getRequiredAttribute(ATTR_SEQUENCE);
        this.modifications = getRequiredAttribute(ATTR_MODIFICATIONS);
    }
    
}