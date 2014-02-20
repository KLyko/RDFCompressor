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
public class IndexRule implements Serializable, Comparable{
	/**
	 * List of Rules which apply to the same subjects.
	 */
	List<IndexRule> parents;
	IndexProfile profile;
	public int nr; 
	
	public IndexRule(IndexProfile profile) {
		this.profile = profile;
		parents = new LinkedList();
	}
	
	public void addParent(IndexRule r) {
		this.parents.add(r);
	}
	
	@Override
	public boolean equals(Object o) {
		return profile.equals(((IndexRule)o).profile);
	}

	@Override
	public int compareTo(Object o) {
		return profile.compareTo(((IndexRule) o).profile);
	}
	
	@Override
	public int hashCode() {
		return profile.hashCode();
	}

    public String toString(){
		String out = this.profile.prop + " - " + this.profile.obj + " - " + this.profile.subjects + ":" ;
		for(IndexRule parent:parents) {
			out+= parent.nr +"|";
		}
		return out.substring(0, out.length()-1);
    }	
    
    public IndexProfile getProfile() {
    	return profile;
    }
    
    public List<IndexRule> getParents() {
    	return parents;
    }
	
}
