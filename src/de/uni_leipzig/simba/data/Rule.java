package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * p-o-pair := {res1, ..., resn}
 * superrules
 * @author Klaus Lyko
 *
 */
public class Rule implements Serializable{

	List<Rule> parents;
	Profile profile;
	
	
	public Rule(Profile profile) {
		this.profile = profile;
		parents = new LinkedList<Rule>();
	}
	
	public void addParent(Rule r) {
		this.parents.add(r);
	}
	
	@Override
	public boolean equals(Object o) {
		return profile.equals(((Rule)o).profile);
	}
}
