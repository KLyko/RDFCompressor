package de.uni_leipzig.simba.data;
/**
 * The profile of a resource s is the set of all property-object pairs of all triples the resource s has in knowledge base K. 
 * profile(s) = {(p,o) : <s,p,o> \in K }
 * @author Klaus Lyko
 *
 */
public interface Profile {
	/**
	 * 
	 * ACHTUNG anders als im Entwurf !!!
	 * Property prop
	 * Resource object
	 * Set<Resource> subjects
	 */
}
