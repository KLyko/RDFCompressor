package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Rules contain a Profile which basically resambles an tuple of a resource property and an object with a set
 * of subjects (Resources):
 * May links to super rules: rules with apply to the at least same subjects as this one..
 * @author Klaus Lyko
 *
 */
public class Rule implements Serializable, Comparable{
	/**
	 * List of Rules which apply to the same subjects.
	 */
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

	@Override
	public int compareTo(Object o) {
		return profile.compareTo(((Rule) o).profile);
	}
	
	
	
}
