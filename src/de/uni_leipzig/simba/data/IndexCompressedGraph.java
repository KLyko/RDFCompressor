package de.uni_leipzig.simba.data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import orestes.bloomfilter.BloomFilter;

import org.apache.log4j.Logger;


/**
 * Default implementation of the CompressedGraph.
 * @author Klaus Lyko
 *
 */
public class IndexCompressedGraph implements CompressedGraph<IndexRule>{
	/**redundant for now*/
	List<IndexRule> rules;
	/**maps pID -> oID -> Rule*/
	HashMap<Integer, HashMap<Integer, IndexRule>> ruleMap;
	
	/**remeber rules subjects are part of*/
	public HashMap<Integer, HashSet<IndexRule>> subjectToRule = new HashMap<Integer, HashSet<IndexRule>>();
	
	static Logger logger = Logger.getLogger(IndexCompressedGraph.class);
	boolean useBloom = true;
	BloomFilter<String> bloom;
	public String log ="";
	
	public IndexCompressedGraph(double expRules, boolean useBloom) {
		rules = new LinkedList<IndexRule>();
		ruleMap = new HashMap();
//		ruleHash = new HashSet<IndexRule>();
		this.useBloom = useBloom;
		bloom = new BloomFilter<String>(expRules, 0.1);
	}

	
	public void addRule(IndexRule r, Integer subject) throws Exception {
		if(useBloom) {
			if(bloom.contains(r.profile.prop+"-"+r.profile.obj)) {
				if(!ruleMap.containsKey(r.profile.prop)) {
					HashMap<Integer, IndexRule> subMap = new HashMap<Integer, IndexRule>();
					subMap.put(r.profile.obj, r);
					ruleMap.put(r.profile.prop, subMap);
					logger.info("Add new rule "+r+" ");
					addSubjectToRuleEntry(r, subject);
				} else { // property does exist				
				IndexRule o = ruleMap.get(r.profile.prop).get(r.profile.obj);
					if(o == null) { // false positive check
						logger.error("False positive on bloom filer");
						bloom.add(r.profile.prop+"-"+r.profile.obj);
						ruleMap.get(r.profile.prop).put(r.profile.obj, r);
						addSubjectToRuleEntry(r, subject);
					} else {
						if(!o.equals(r)) {//FIXME Doesn't work!!!
							System.err.println("Retrieved rule isn't the same");
							throw new Exception("Retrieved rule isn't the same");
						}
						
						o.profile.subjects.addAll(r.profile.subjects);
						logger.info("Found existing rule "+o+" "+subject);
						addSubjectToRuleEntry(o, subject);
					}
				}
			} else { // not found in bloom
				bloom.add(r.profile.prop+"-"+r.profile.obj);
				logger.info("Not in bloom: adding new rule"+r);
				if(!ruleMap.containsKey(r.profile.prop))
					ruleMap.put(r.profile.prop, new HashMap<Integer, IndexRule>());
				ruleMap.get(r.profile.prop).put(r.profile.obj, r);
				addSubjectToRuleEntry(r, subject);
			}
		} else {//  no bloom
				if(!ruleMap.containsKey(r.profile.prop)) {
					HashMap<Integer, IndexRule> subMap = new HashMap<Integer, IndexRule>();
					subMap.put(r.profile.obj, r);
				} 
				else {
					IndexRule o = ruleMap.get(r.profile.prop).get(r.profile.obj);
					if(o == null) {
						ruleMap.get(r.profile.prop).put(r.profile.obj, r);		
						addSubjectToRuleEntry(r, subject);
					}
					if(!o.equals(r)) {
						System.err.println("Retrieved rule isn't the same");
						throw new Exception("Retrieved rule isn't the same");
					}
					o.profile.subjects.addAll(r.profile.subjects);
					addSubjectToRuleEntry(o, subject);
				}
		}
		
	}
	
	@Deprecated
	public Set<IndexRule> getSuperRules(IndexRule r) {
		HashSet<IndexRule> result = new HashSet<IndexRule>();
		// Collections.sort(rules);
		for(IndexRule o : rules) {
//			IndexRule o = e.getValue();
			if(o.profile.size()<r.profile.size())
				continue;
			if(r.profile.subjects.isEmpty())
				continue;
			else {// other has almost as many elements
				if(!r.profile.equals(o.profile))// isn't the same
//				    if(!r.profile.subjects.isEmpty() ) // isn't empty
						if(r.profile.min>=o.profile.min && // check uri ranges
							r.profile.max<=o.profile.max)
							if(!o.parents.contains(r)) // avoid double linking to parent
							if(o.profile.containsAll(r.profile)) // other contains all uris of r
							{
									result.add(o);				
							}
			}
		}
		return result;
	}

	@Override
	public void computeSuperRules() {
		long start = System.currentTimeMillis();
		rules = new ArrayList<IndexRule>(rules.size());
		for(Entry<Integer, HashMap<Integer, IndexRule>> e : ruleMap.entrySet()) {
			rules.addAll(e.getValue().values());
		}
	
    	Collections.sort(rules, new IndexRuleComparator());
    	long mid = System.currentTimeMillis();
    	for(IndexRule r : rules) {
    		r.setNumber(rules.indexOf(r));
    	}
    	
    	
    	long end = System.currentTimeMillis();
    	start = end;
    	log+="\n\tSorting rules by property:"+(mid-start)+" ms = "+((mid-start)/1000)+" s";
    	log+="\n\tAssigning new rule numbers:"+(end-mid)+" ms = "+((end-mid)/1000)+" s";
//		Collections.sort(rules); // O(n*log n)
		//1st compute all supersets
		for(IndexRule r : rules) { //O(n²)
			if(r.getProfile().subjects.size()>1) {
//				Set<IndexRule> supersets = getSuperRules(r);
//				r.parents.addAll(supersets);
				Set<IndexRule> parents = computeFeasibleSuperRules(r);
				r.parents.addAll(parents);
			}
		
		}
		
		end = System.currentTimeMillis();
		log+="\n\tComputing super rules took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		//2nd remove redundant uris in supersets
		start = end;
		for(IndexRule r : rules) { //O(n)
			for(IRule<IndexProfile> superRule : r.parents) {
				superRule.getProfile().subjects.removeAll(r.profile.subjects);
			}
		}
		end = System.currentTimeMillis();
		log+="\n\tRemoving subjects took "+(end-start)+" ms = "+((end-start)/1000) +" s";
	}

	@Override
	public void removeRedundantParentRules() {
		long start = System.currentTimeMillis();
		for(IndexRule r : rules) {
			List<IRule<IndexProfile>> copy = new LinkedList();
			copy.addAll(r.parents);
			for(IRule<IndexProfile> parent : r.parents)
				copy.removeAll(parent.getParents());
			r.parents = copy;
		}
		long end = System.currentTimeMillis();
		log+="\nRemoving redundant parents took "+(end-start)+" ms = "+((end-start)/1000) +" s";
	}
	
    public String toString(){
	String s = "";
	for (IndexRule rule : this.rules){
	    s += rule + "\n";
	}
	return s;
    }
	
	public int size() {
		int s=0;
		for(IndexRule r : rules) {
			s+= r.profile.size();
		}
		return s;
	}

    public String serialize(){
	String s = "";
	for (IndexRule rule : this.rules){
	    s += rule.profile.prop + "|" + rule.profile.obj;
	    for (Integer subject : rule.profile.subjects){
		s += "|" + subject;
	    }
	s += "\n";
	}
	return s;
    }
    
    public List<IndexRule> getRules() {
    	return rules;
    }
   
    /**
     * Adds a map entry for the new triple.
     * @param rule
     * @param subject
     */
    private void  addSubjectToRuleEntry(IndexRule rule, Integer subject) {
    	if(subjectToRule.containsKey(subject)) {
    		subjectToRule.get(subject).add(rule);
    	} else {
    		HashSet<IndexRule> rules = new HashSet<IndexRule>();
    		rules.add(rule);
    		subjectToRule.put(subject, rules);
    	}
    }
    
    /**
     * Method to compute SuperRuöes
     * @param r
     * @return
     */
    private Set<IndexRule> computeFeasibleSuperRules(IndexRule r) {
    	HashSet<IndexRule> rules = new HashSet<>();
    	Set<Integer> subs = r.profile.subjects;
    	if(subs.isEmpty())
    		return rules;
    	// non empty subs
    	Iterator<Integer> it = subs.iterator();
    	HashSet<IndexRule> returnSet = new HashSet<>();
    	rules.addAll(subjectToRule.get(it.next()));
    	returnSet.addAll(rules); // need intermediate Set. TODO use other set... 
    	while(it.hasNext() && !rules.isEmpty()) {
    		Set<IndexRule> others = subjectToRule.get(it.next());
    		for(IndexRule ri : rules) {
    			if(!others.contains(ri))
    				returnSet.remove(ri);
    		}
    		rules.clear();
    		rules.addAll(returnSet);
    	}
    	returnSet.remove(r); // avoid transitivity
    	return returnSet;
    }
}
 