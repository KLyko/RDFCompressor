package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
	List<IRule<IndexProfile>> children;
	List<Integer> parentsIndices;
	IndexProfile profile;
	int nr;
	public int atomNr; 
	boolean superRulesComputed = false;
	
	public Set<Integer> deleteGraph = new HashSet<Integer>();
	
	public boolean isSuperRulesComputed() {
		return superRulesComputed;
	}
	public void setSuperRulesComputed(boolean superRulesComputed) {
		this.superRulesComputed = superRulesComputed;
	}
	public IndexRule(IndexProfile profile) {
		this.profile = profile;
		parents = new LinkedList();
		children = new LinkedList();
		parentsIndices = new LinkedList();
	}
	@Override
	public void addParent(IRule<IndexProfile> r) {
		this.parents.add(r);
	}
	
	@Override
	public void removeParent(IRule<IndexProfile> r) {
		this.parents.remove(r);
	}
	
	
	@Override
	public void addChild(IRule child) {
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
//	@Override
	public List<IRule<IndexProfile>> getChildren() {
		return children;
	}
	
	public void addParentIndex(int index) {
		this.parentsIndices.add(index);
	}
	
	public List<Integer> getParentIndices() {
		return parentsIndices;
	}
	
	/**
	 * An atomic rule stands for its self: has no parents or children an only one subject.
	 * @return
	 */
	public boolean isAtomic() {
		if(profile.subjects.size() == 1) {
			if(parents.isEmpty())
				if(children.isEmpty())
					return true;
		}
		return false;
	}
}
