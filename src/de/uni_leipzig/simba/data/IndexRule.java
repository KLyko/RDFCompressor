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
public class IndexRule implements IRule<IndexProfile>, Serializable, Comparable{
	/**
	 * List of Rules which apply to the same subjects.
	 */
	List<IRule<IndexProfile>> parents;
	List<IRule<IProfile>> children;
	IndexProfile profile;
	int nr;
	
	public IndexRule(IndexProfile profile) {
		this.profile = profile;
		parents = new LinkedList();
		children = new LinkedList();
	}
	@Override
	public void addParent(IRule<IndexProfile> r) {
		this.parents.add(r);
	}
	
	@Override
	public void addChild(IRule<IProfile> child) {
		this.children.add(child);
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
	@Override
    public String toString(){
		String out = this.profile.prop + " - " + this.profile.obj + " - " + this.profile.subjects + ":" ;
		for(IRule parent:parents) {
			out+= parent.getNumber() +"|";
		}
		return out.substring(0, out.length()-1);
    }	
    @Override
    public IndexProfile getProfile() {
    	return profile;
    }
    @Override
    public List<IRule<IndexProfile>> getParents() {
    	return parents;
    }

	@Override
	public void setProfile(IndexProfile profile) {
		this.profile = profile;
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
	public List<IRule<IProfile>> getChildren() {
		return children;
	}
}
