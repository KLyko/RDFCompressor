package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * As of now a Profile is the basic representation of a rule.
 * ACHTUNG anders als im Entwurf !!!
 * Property prop
 * Resource object
 * Set<Resource> subjects
 */



public class Profile implements Serializable, Comparable {

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
		return(prop.getURI().equalsIgnoreCase(o.prop.getURI()) && obj.equals(o.obj));
	}
	
	/**
	 * Size of a Profile is it's number of subjects
	 * @return
	 */
	public int size() {
		return subjects.size();
	}
	
	@Override
	public int compareTo(Object o) {
		return this.size()-((Profile) o).size();
	}
	
	@Override
	public int hashCode() {
		return (prop.toString()+obj.toString()).hashCode();
	}
}
