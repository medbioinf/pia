package de.mpc.pia.webgui.component;

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.TagAttribute;

public class ProteinSequenceWithCoverageTagHandler extends ComponentHandler {
	
    private static final String ATTR_SEQUENCE = "sequence";
    
    private static final String ATTR_COVERAGEMAP = "coveragemap";
    
    private static final String ATTR_LINEWIDTH = "linewidth";
    
    
    private TagAttribute sequence;
    
    private TagAttribute coveragemap;
    
    private TagAttribute linewidth;
    
    
    public ProteinSequenceWithCoverageTagHandler(ComponentConfig config) {
        super(config);
        this.sequence = getRequiredAttribute(ATTR_SEQUENCE);
        this.coveragemap = getRequiredAttribute(ATTR_COVERAGEMAP);
        this.linewidth = getAttribute(ATTR_LINEWIDTH);
    }
    
}