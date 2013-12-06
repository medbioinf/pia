package de.mpc.pia.webgui.component;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

import org.richfaces.renderkit.html.PanelRenderer;

import java.io.IOException;
import java.util.Iterator;


@FacesRenderer(componentFamily = "org.richfaces.Panel",
	rendererType = "de.mpc.pia.webgui.component.ProteinSequenceWithCoverage")
public class ProteinSequenceWithCoverageRenderer extends PanelRenderer {
	
	private final static int WHITESPACE     = 1;
	private final static int START_COVERAGE = 2;
	private final static int END_COVERAGE   = 3;
	private final static int END_SEQUENCE   = 4;
	private final static int LINEBREAK      = 5;
	
	
	@Override
	public void renderChildren(FacesContext context, UIComponent component)
			throws IOException {
		if (component instanceof ProteinSequenceWithCoverage) {
			renderProteinSequence(context,
					(ProteinSequenceWithCoverage)component);
		}
	}
	
	
	/**
	 * Renders the protein sequence into the panel with highlighted areas for
	 * amino acids covered by proteins.
	 * 
	 * @param context
	 * @param panel
	 * @param spectrum
	 */
	private void renderProteinSequence(FacesContext context,
			ProteinSequenceWithCoverage component) throws IOException {
		
		String sequence = component.getSequence();
		if (sequence == null) {
			return;
		}
		
		ResponseWriter writer = context.getResponseWriter();
		
		Integer pos = 1;
		Integer lineWidth = (int)(component.getLineWidth() * 10 / 10);	// round it to 10
		Integer spanBegin = sequence.length();
		Integer spanEnd = sequence.length();
		Integer nextSpace = 10;
		Integer nextAction = WHITESPACE;
		boolean inSpan = false;
		
		Iterator<Integer> coverageIt = component.getCoverageMap().navigableKeySet().iterator();
		if (coverageIt.hasNext()) {
			spanBegin = coverageIt.next();
			spanEnd = component.getCoverageMap().get(spanBegin);
		}
		
		if (spanBegin < nextSpace) {
			nextAction = START_COVERAGE;
		}
		
		writer.startElement("pre", component);
		
		for (int p=nextSpace;
				(p <= lineWidth) && (p <= sequence.length());
				p += 10) {
			writer.writeText(String.format("%10d ", p), null);
		}
		writer.writeText("\n", null);
		
		while (nextAction != END_SEQUENCE) {
			switch (nextAction) {
			case START_COVERAGE:
				if (pos < spanBegin) {
					writer.writeText(sequence.substring(pos-1, spanBegin-1), null);
					pos = spanBegin;
				}
				writer.startElement("b", component);
				writer.writeAttribute("class", "covered", null);
				inSpan = true;
				
				if (spanEnd <= nextSpace) {
					nextAction = END_COVERAGE;
				} else {
					nextAction = WHITESPACE;
				}
				break;
				
			case END_COVERAGE:
				if (pos <= spanEnd) {
					writer.writeText(sequence.substring(pos-1, spanEnd), null);
					pos = spanEnd+1;
				}
				writer.endElement("b");
				inSpan = false;
				
				if (coverageIt.hasNext()) {
					spanBegin = coverageIt.next();
					spanEnd = component.getCoverageMap().get(spanBegin);
				} else {
					spanBegin = sequence.length()+100;
					spanEnd = spanBegin;
				}
				
				if (spanBegin <= nextSpace) {
					nextAction = START_COVERAGE;
				} else if (sequence.length() < nextSpace) {
					nextAction = END_SEQUENCE;
				} else {
					nextAction = WHITESPACE;
				}
				break;
				
			case WHITESPACE:
				if (pos <= nextSpace) {
					writer.writeText(sequence.substring(pos-1, nextSpace), null);
					pos = nextSpace+1;
				}
				
				if (nextSpace % lineWidth == 0) {
					nextAction = LINEBREAK;
				} else {
					writer.writeText(" ", null);
					nextSpace += 10;
					
					if (inSpan && (spanEnd < nextSpace)) {
						nextAction = END_COVERAGE;
					} else if (!inSpan && (spanBegin <= nextSpace)) {
						nextAction = START_COVERAGE;
					} else if (sequence.length() < nextSpace) {
						nextAction = END_SEQUENCE;
					} else {
						nextAction = WHITESPACE;
					}
				}
				break;
				
			case LINEBREAK:
				if (inSpan) {
					writer.endElement("b");
				}
				
				writer.writeText("\n\n", null);
				nextSpace += 10;
				
				for (int p=nextSpace;
						(p < nextSpace + lineWidth) && (p <= sequence.length());
						p += 10) {
					writer.writeText(String.format("%10d ", p), null);
				}
				writer.writeText("\n", null);
				
				if (inSpan) {
					writer.startElement("b", component);
					writer.writeAttribute("class", "covered", null);
				}
				
				if (inSpan && (spanEnd < nextSpace)) {
					nextAction = END_COVERAGE;
				} else if (!inSpan && (spanBegin <= nextSpace)) {
					nextAction = START_COVERAGE;
				} else if (sequence.length() < nextSpace) {
					nextAction = END_SEQUENCE;
				} else {
					nextAction = WHITESPACE;
				}
				break;
			}
		}
			
		writer.writeText(sequence.substring(pos-1, sequence.length()), null);
		writer.endElement("pre");
	}
}
