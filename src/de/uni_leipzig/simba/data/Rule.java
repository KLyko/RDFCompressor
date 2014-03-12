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
public class Rule implements IRule<Profile>, Serializable, Comparable{
	/**
	 * List of Rules which apply to the same subjects.
	 */
	List<IRule<Profile>> parents;
	List<IRule<IProfile>> children;
	Profile profile;
	int nr;
	
	public Rule(Profile profile) {
		this.profile = profile;
		parents = new LinkedList();
		children = new LinkedList();
	}
	@Override
	public void addParent(IRule r) {
		this.parents.add(r);
	}
	
	@Override
	public void addChild(IRule r) {
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
	
	@Override
	public int hashCode() {
		return profile.hashCode();
	}
	@Override
    public String toString(){
	return this.profile.prop + " - " + this.profile.obj + " - " + this.profile.subjects;
    }

	@Override
	public int getNumber() {
		return nr;
	}

	@Override
	public void setNumber(int nr) {
		this.nr = nr;
	}

	@Override
	public void setProfile(Profile profile) {
		this.profile= profile; 
	}

	@Override
	public Profile getProfile() {
		return profile;
	}

	@Override
	public List<IRule<Profile>> getParents() {
		return parents;
	}	
	
	@Override
	public List<IRule<IProfile>> getChildren() {
		return children;
	}
}
