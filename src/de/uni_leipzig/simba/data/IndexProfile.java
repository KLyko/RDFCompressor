package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Profile using Indexes
 * @author Klaus Lyko
 *
 */
public class IndexProfile implements IProfile<Integer, Integer, Integer>, Serializable, Comparable {

	Integer prop;
	Integer obj;
	Set<Integer> subjects = new HashSet<Integer>();
	Integer min=Integer.MAX_VALUE;
	Integer max=Integer.MIN_VALUE;
	
	
	public IndexProfile(Integer prop, Integer obj) {
		this.prop = prop;
		this.obj = obj;
		subjects = new HashSet<Integer>();
	}
	
	public boolean addSubject(Integer r) {
		if(min > r)
			min = r;
		if(max < r)
			max = r;
		return subjects.add(r);
	}
	
	@Override
	public boolean equals(Object other) {
		IndexProfile o = (IndexProfile) other;
		return(prop.equals(o.prop) && obj.equals(o.obj));
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
		return this.size()-((IndexProfile) o).size();
	}
	
	@Override
	public int hashCode() {
		return (""+prop.toString()+obj.toString()).hashCode();
	}
	
	public Integer getProperty() {
		return prop;
	}
	public Integer getObject() {
		return obj;
	}
	public Set<Integer> getSubjects() {
		return subjects;
	}

	@Override
	public void setObject(Integer object) {
		obj = object;
	}

	@Override
	public void setProperty(Integer property) {
		prop = property;
	}
	@Override
	public Integer getMinSubject() {
		return min;
	}
	@Override
	public Integer getMaxSubject() {
		return max;
	}
}
