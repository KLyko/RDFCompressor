package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The profile of a resource s is the set of all property-object pairs of all triples the resource s has in knowledge base K. 
 * profile(s) = {(p,o) : <s,p,o> \in K }
 * @author Klaus Lyko
 *
 */
public class Profile implements Serializable {
	/**
	 * 
	 * ACHTUNG anders als im Entwurf !!!
	 * Property prop
	 * Resource object
	 * Set<Resource> subjects
	 */
	
	
	
	Property prop;
	RDFNode obj;
	Set<Resource> subjects;
	
	public Profile(Property prop, RDFNode obj) {
		this.prop = prop;
		this.obj = obj;
		subjects = new HashSet<Resource>();
	}
	
	public boolean addSubject(Resource r) {
		return subjects.add(r);
	}
	
	@Override
	public boolean equals(Object other) {
		Profile o = (Profile) other;
		return(prop.equals(o.prop) && obj.equals(o.obj));
	}
}
