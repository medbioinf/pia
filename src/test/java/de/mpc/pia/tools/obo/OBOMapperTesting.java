package de.mpc.pia.tools.obo;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Set;

import org.biojava.nbio.ontology.Term;
import org.junit.Test;

public class OBOMapperTesting {
	@Test
	public void readOBO() {
		OBOMapper oboMapper = new OBOMapper();
		
		boolean foundTrypsin = false;
		boolean foundMascotScore = false;
		boolean foundMSGF = false;
		
		Set<Term> keys = oboMapper.getTerms();
		Iterator<Term> iter = keys.iterator();
		while (iter.hasNext()){
			Term term = (Term) iter.next();
			
			if (term.getName().equals("MS:1001176")) {
				foundTrypsin = true;
			}
			
			if ((term.getDescription() != null) &&
					(term.getDescription().equals("Mascot:score"))) {
				foundMascotScore = true;
			}
			
			if (term.getName().equals("MS:1002048")) {
				foundMSGF = true;
			}
		}
		
		assertEquals("trypsin (MS:1001176) should be found in the obo", true, foundTrypsin);
		assertEquals("Mascot:score should be found in the obo", true, foundMascotScore);
		assertEquals("MS/GF+ (MS:1002048) should be found in the obo", true, foundMSGF);
	}
}