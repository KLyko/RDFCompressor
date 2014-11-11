package de.uni_leipzig.simba.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Compressedgraph holding and managing all rules. As rules are supposed to be added complete (with all subjects)
 * A lot of redundancy checks are not neccessary, as they are added. Also computing all super rules is
 * easier as they are already ordered by their property ({@link ModelCompressor)
 * @author Klaus Lyko
 *
 */
public class InstantCompressedGraph extends IndexCompressedGraph{

	
	public InstantCompressedGraph(double expRules, boolean useBloom,
			int deleteBorder) {
		super(expRules, useBloom, deleteBorder);
	}

	public void addRule(IndexRule r, Set<Integer> subs) throws Exception {
		if(!ruleMap.containsKey(r.profile.prop)) {
			HashMap<Integer, IndexRule> subMap = new HashMap<Integer, IndexRule>();
			subMap.put(r.profile.obj, r);
			ruleMap.put(r.profile.prop, subMap);
			for(Integer subject:subs)
				addSubjectToRuleEntry(r, subject);
		} else { // property does exist				
			IndexRule o = ruleMap.get(r.profile.prop).get(r.profile.obj);
			if(o == null) { // false positive check
				ruleMap.get(r.profile.prop).put(r.profile.obj, r);
				for(Integer subject:subs)
					addSubjectToRuleEntry(r, subject);
			} else {
				if(!o.equals(r)) {
					System.err.println("Retrieved rule isn't the same");
					throw new Exception("Retrieved rule isn't the same");
				}
				for(Integer sub : r.profile.subjects) {
					o.profile.addSubject(sub);
					addSubjectToRuleEntry(o, sub);
				}
			}
		}
			
		r.nr = rules.size();
		rules.add(r);		
	}
	
	
	public void computeSuperRules(IndexRule r) {
		for(int i = 0; i<rules.size(); i++) {//for every rule
			IndexRule o = rules.get(i);
			if(r.profile.size() >= o.profile.size()) {
				// r is a possible super rule
				if(r.profile.min <= o.profile.min)
					if(r.profile.max >= o.profile.max)
//						if(!o.parents.contains(r)) // not neccesary, should not be possible
							if(r.profile.subjects.containsAll(o.profile.subjects)) {
								System.out.println("Found children: "+o+"  < "+r);
								o.parents.add(r);
								r.children.add(o);
							}
			} else { // other is a potential super rule
				if(o.profile.min <= r.profile.min)
					if(o.profile.max >= r.profile.max)
						if(!r.parents.contains(o))
							if(o.profile.subjects.containsAll(r.profile.subjects)) {
								r.parents.add(o);
								o.children.add(r);
								r.parents.addAll(o.parents);
							}
			}
			r.superRulesComputed = true;
		}
	}
	
	public void computeAllSuperRulesOnce() {
		long start = System.currentTimeMillis();
		if(this.deleteBorder <= 0)
			Collections.sort(rules); // O(n*log n). Order by size
		long end = System.currentTimeMillis();
		
    	log+="\n\tSorting rules by size:"+(end-start)+" ms = "+((end-start)/1000)+" s";
    	System.out.println("\n\tSorting rules by size:"+(end-start)+" ms = "+((end-start)/1000)+" s");
		
		
		//1st compute all supersets
		String  println = "\n\tComputing super rules by iterating over all rules ";
		if(this.deleteBorder <= 0) {
	    	if(rules.size()<500) { // if we only have some rules use normal approach: iterate over all other rules.
	    		println += " with subject-set based approach.";
	    		for(int i = 0; i<rules.size(); i++) { //O(n²)
	    			IndexRule r = rules.get(i);
	    			if(r.getProfile().subjects.size()>1) {
	    				Set<IndexRule> supersets = getSuperRules(r, i);
						r.parents.addAll(supersets);
						r.setSuperRulesComputed(true);
	    			}
	    		}
	    	} else { // many rules use
	    		println = "Computing super rules by with subject to Rules map.";
	    		for(IndexRule r : rules) { 
	    			if(r.getProfile().subjects.size()>1) {
		    			Set<IndexRule> parents = computeFeasibleSuperRules(r);
		    			for(IndexRule pp : parents)
		    				if(!pp.getParents().contains(r))
		    					r.parents.add(pp);
		    				else {
		//    					System.out.println("Doesn't add " + pp +" to " + r + "parents");
		    				}
		    			r.setSuperRulesComputed(true);
	    			}
	    		}
	    	}
		} else { //compute delete rule
			for(IndexRule r : rules) { 
    			if(r.getProfile().subjects.size()>1) {
    				HashMap<Integer, RuleToDeleteGraph> dels = computeDeleteBasedRules(r, this.deleteBorder);
    				for(Entry<Integer, RuleToDeleteGraph> entry : dels.entrySet()) {
    					IndexRule parent = rules.get(entry.getKey());
    					if(!parent.parents.contains(r) && parent.compareTo(r)>=0) {
	    					RuleToDeleteGraph rToDelGraph = entry.getValue();
	//    					rToDelGraph.
	    					parent.deleteGraph.addAll(rToDelGraph.notIn);
	    					parent.addChild(r);
	    					r.addParent(parent);
    					}
    				}
    			}
			}
			
		}
		end = System.currentTimeMillis();
		println+=" took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		log+=println;
		System.out.println(println);
		
		
		//2nd remove redundant uris in supersets
		start = System.currentTimeMillis();
		for(IndexRule r : rules) { //O(n)
			for(IRule<IndexProfile> superRule : r.parents) {
				superRule.getProfile().subjects.removeAll(r.profile.subjects);
			}
		}
		end = System.currentTimeMillis();
		log+="\n\tRemoving subjects took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		System.out.println("\n\tRemoving subjects took "+(end-start)+" ms = "+((end-start)/1000) +" s");
		start = System.currentTimeMillis();
		Collections.sort(rules, new Comparator() {
            public int compare(Object o1, Object o2)             {
                if(o1 instanceof IndexRule && o2 instanceof IndexRule) {
                	return ((IndexRule)o1).nr-((IndexRule)o2).nr;
                }
                System.err.println("Trying to compare unknown objects.");
                return 0;
            }
           }    );
		end = System.currentTimeMillis(); 
		log+="\n\tResorting by property took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		System.out.println("\n\tResorting by property took "+(end-start)+" ms = "+((end-start)/1000) +" s");
	}
}
