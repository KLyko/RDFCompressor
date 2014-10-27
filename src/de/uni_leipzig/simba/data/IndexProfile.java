package de.uni_leipzig.simba.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import orestes.bloomfilter.BloomFilter;

/**
 * Profile using Indexes
 * @author Klaus Lyko
 *
 */
public class IndexProfile implements IProfile<Integer, Integer, Integer>, Serializable, Comparable {

	static Logger logger = Logger.getLogger(IndexCompressedGraph.class);
	Integer prop;
	Integer obj;
	Set<Integer> subjects = new HashSet<Integer>();
	Integer min=Integer.MAX_VALUE;
	Integer max=Integer.MIN_VALUE;
	BloomFilter<Integer> bloom;
	public int errorRate = 0;
	
	public IndexProfile(Integer prop, Integer obj) {
		this.prop = prop;
		this.obj = obj;
		subjects = new HashSet<Integer>();
		bloom = new BloomFilter<Integer>(100, 0.1);
	}
	
	public boolean addSubject(Integer r) {
		if(min > r)
			min = r;
		if(max < r)
			max = r;
		bloom.add(r);
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
		return (1/2)*(prop+obj)*(prop+obj+1)+obj;
//		pi(k1, k2) = 1/2(k1 + k2)(k1 + k2 + 1) + k2
//		return (""+prop.toString()+obj.toString()).hashCode();
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
	
	/**
	 * Checks whether all Uris of Profile2 are contained within here first using its bloomfilter.
	 * @param profile2
	 * @return
	 */
	public boolean containsAll(IndexProfile profile2) {
		
		if(!bloom.containsAll(profile2.getSubjects())) {
//			System.out.println("Testing whether "+subjects+" containsAll "+profile2.subjects+" bloom said no");
			return false;
		} else {
			boolean answer2 = subjects.containsAll(profile2.subjects);
			if(!answer2) {
				errorRate++;
				logger.debug("False Positive on BloomFilter URIs contains check");
			}
			return answer2;				
		}
	}
	
	public String debugOutPut() {
		return "min="+min+" max="+max;
	}
}
