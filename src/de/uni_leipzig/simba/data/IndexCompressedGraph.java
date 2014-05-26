package de.uni_leipzig.simba.data;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
	
	public int deleteBorder = 0;
	
	/**remeber rules subjects are part of*/
	public HashMap<Integer, HashSet<IndexRule>> subjectToRule = new HashMap<Integer, HashSet<IndexRule>>();
	
	static Logger logger = Logger.getLogger(IndexCompressedGraph.class);
	boolean useBloom = true;
	BloomFilter<String> bloom;
	public String log ="";
//	public HashMap<Integer, Integer> newToOld = new HashMap();
	
	
	public IndexCompressedGraph(double expRules, boolean useBloom, int deleteBorder) {
		rules = new LinkedList<IndexRule>();
		ruleMap = new HashMap();
//		ruleHash = new HashSet<IndexRule>();
		this.useBloom = useBloom;
		this.deleteBorder = deleteBorder;
		bloom = new BloomFilter<String>(expRules, 0.1);
	}

	
	public void addRule(IndexRule r, Integer subject) throws Exception {
		if(useBloom) {
			if(bloom.contains(r.profile.prop+"-"+r.profile.obj)) {
				if(!ruleMap.containsKey(r.profile.prop)) {
					HashMap<Integer, IndexRule> subMap = new HashMap<Integer, IndexRule>();
					subMap.put(r.profile.obj, r);
					ruleMap.put(r.profile.prop, subMap);
//					logger.info("Add new rule "+r+" ");
					addSubjectToRuleEntry(r, subject);
				} else { // property does exist				
				IndexRule o = ruleMap.get(r.profile.prop).get(r.profile.obj);
					if(o == null) { // false positive check
//						logger.error("False positive on bloom filer");
						bloom.add(r.profile.prop+"-"+r.profile.obj);
						ruleMap.get(r.profile.prop).put(r.profile.obj, r);
						addSubjectToRuleEntry(r, subject);
					} else {
						if(!o.equals(r)) {
							System.err.println("Retrieved rule isn't the same");
							throw new Exception("Retrieved rule isn't the same");
						}
						
//						o.profile.subjects.addAll(r.profile.subjects);
						for(Integer sub : r.profile.subjects)
							o.profile.addSubject(sub);
//						logger.info("Found existing rule "+o+" "+subject);
						addSubjectToRuleEntry(o, subject);
					}
				}
			} else { // not found in bloom
				bloom.add(r.profile.prop+"-"+r.profile.obj);
//				logger.info("Not in bloom: adding new rule"+r);
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
					for(Integer sub : r.profile.subjects)
						o.profile.addSubject(sub);
					addSubjectToRuleEntry(o, subject);
				}
		}
		
	}
	

	public Set<IndexRule> getSuperRules(IndexRule r) {
		HashSet<IndexRule> result = new HashSet<IndexRule>();
		// Collections.sort(rules);
		for(IndexRule o : rules) {
//			System.out.println("\tComparing "+r.getProfile().subjects+" with "+o.getProfile().subjects);

//			IndexRule o = e.getValue();
			if(o.profile.size()<r.profile.size())
				continue;
			if(r.profile.subjects.isEmpty())
				continue;
			else {// other has almost as many elements
				if(!r.profile.equals(o.profile)) {// isn't the same
//					System.out.println("\t pofiles doesn't equal");
//				    if(!r.profile.subjects.isEmpty() ) // isn't empty
//						System.out.println("\t r: "+r.profile.debugOutPut()+" o: "+o.profile.debugOutPut());
						if(r.profile.min>=o.profile.min && // check uri ranges
							r.profile.max<=o.profile.max) {
//							System.out.println("\t min max okay");
							if(!o.parents.contains(r))  {// avoid double linking to parent
//								System.out.println("\t o doesn't contain r");
								if(o.profile.containsAll(r.profile)) // other contains all uris of r
								{
										result.add(o);
										if(o.isSuperRulesComputed()) {
											result.addAll((Collection<? extends IndexRule>) o.parents);
//											System.out.println("\tAvoid further computation");
											return result;
										}
								}
							}
						}
				}
			}
		}
		return result;
	}

	@Override
	public void computeSuperRules() {
		long start = System.currentTimeMillis();
		rules = new ArrayList<IndexRule>(ruleMap.size());
		for(Entry<Integer, HashMap<Integer, IndexRule>> e : ruleMap.entrySet()) {
			rules.addAll(e.getValue().values());
		}
		// sort rules based on property => to save space
    	Collections.sort(rules, new IndexRulePropertyComparator());
    	long mid = System.currentTimeMillis();
    	log+="\n\tSorting rules by property:"+(mid-start)+" ms = "+((mid-start)/1000)+" s";
    	System.out.println("\n\tSorting rules by property:"+(mid-start)+" ms = "+((mid-start)/1000)+" s");
    	
    	for(IndexRule r : rules) {
    		r.setNumber(rules.indexOf(r));
    	}    	
    	long end = System.currentTimeMillis();
    	start = end;
    	log+="\n\tAssigning new rule numbers:"+(end-mid)+" ms = "+((end-mid)/1000)+" s";
    	System.out.println("\n\tAssigning new rule numbers:"+(end-mid)+" ms = "+((end-mid)/1000)+" s");
		Collections.sort(rules); // O(n*log n)
		//1st compute all supersets
		String  println = "\n\tComputing super rules ";
		if(this.deleteBorder < 0) {
	    	if(rules.size()<500) {
	    		println += " with subject-set based approach.";
	    		for(IndexRule r : rules) { //O(n²)
	    			if(r.getProfile().subjects.size()>1) {
	//    			System.out.println("Computing super rules of "+r);
	    				Set<IndexRule> supersets = getSuperRules(r);
//	    				HashMap<Integer, RuleToDeleteGraph> deleteRules = computeDeleteBasedRules(r);
//	    				System.out.println("delete Rules for "+r+":");
//	    				for(Entry<Integer, RuleToDeleteGraph> e : deleteRules.entrySet()) {
//	    					System.out.println("\t"+e.getValue().parent+" NOT:"+e.getValue().notIn);
//	    				}
//	    				
	//    				System.out.println("result:"+supersets);
						r.parents.addAll(supersets);
						r.setSuperRulesComputed(true);
	    			}
	    		}
	    	} else {
	    		println += " with subject to Rules map.";
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
		} else {
			println += " with delete based approach border="+deleteBorder+".";
			for(IndexRule r : rules) { //O(n²)
				if(r.getProfile().subjects.size()>0) {
//					System.out.println("Computing super rules for "+r);
			    	HashMap<Integer, RuleToDeleteGraph> deleteRules = computeDeleteBasedRules(r, deleteBorder);
//					System.out.println("delete Rules for "+r+":");
					for(Entry<Integer, RuleToDeleteGraph> e : deleteRules.entrySet()) {
						RuleToDeleteGraph parent = e.getValue();
						IndexRule parentRule = parent.parent;
						if(!parentRule.getParents().contains(r)) {
							parentRule.deleteGraph.addAll(parent.notIn);
							r.parents.add(parentRule);
							parentRule.children.add(r);
						} else {
//							System.out.println("Avoiding edge!");
						}
//						System.out.println("\t"+e.getValue().parent+" NOT:"+e.getValue().notIn);
					}
					r.superRulesComputed = true;
				}
			}
		}
    	

    	
		end = System.currentTimeMillis();
		
//		if(rules.size()<500) 
//			println+=" using quadratic approach ";
//		else
//			println+=" using subject to rule map ";
		println+=" took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		log+=println;
		System.out.println(println);
		
		
		//2nd remove redundant uris in supersets
		start = end;
		for(IndexRule r : rules) { //O(n)
			for(IRule<IndexProfile> superRule : r.parents) {
//				System.out.println("Removing "+r.profile.subjects+" from "+superRule.getProfile().subjects);
				superRule.getProfile().subjects.removeAll(r.profile.subjects);
			}
		}
		end = System.currentTimeMillis();
		log+="\n\tRemoving subjects took "+(end-start)+" ms = "+((end-start)/1000) +" s";
		System.out.println("\n\tRemoving subjects took "+(end-start)+" ms = "+((end-start)/1000) +" s");
		// remove siblings
//		removeSiblingRules();
		Collections.sort(rules, new Comparator() {

            public int compare(Object o1, Object o2) 
            {
                if(o1 instanceof IndexRule && o2 instanceof IndexRule) {
                	return ((IndexRule)o1).nr-((IndexRule)o2).nr;
                }
                System.err.println("Trying to compare unknown objects.");
                return 0;
            }
           }    );
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
//	/**
//	 * For two rules 
//	 */
//	public void removeSiblingRules() {
//		long start = System.currentTimeMillis();
//		for(IndexRule r : rules) {
//			List<IRule<IndexProfile>> copy = new LinkedList();
//			copy.addAll(r.children);
//			List<IRule<IndexProfile>> children = r.children;
//			for(int i = 0; i<children.size(); i++) {	
//				for(int j=i; j<children.size(); j++) {
//					if(!children.get(i).equals(children.get(j))) {
//						IndexProfile ip = children.get(i).getProfile();
//						IndexProfile jp = children.get(j).getProfile();
//						if(!ip.subjects.isEmpty() && !jp.subjects.isEmpty())
//							if(ip.subjects.containsAll(jp.getSubjects())) {
//								copy.remove(children.get(j));
//								r.children.get(j).removeParent(r);
//								System.out.println("Found Sibling of "+r.children.get(i)+": "+r.children.get(j));
//							} else if(jp.subjects.containsAll(ip.getSubjects())){
//								copy.remove(children.get(i));
//								r.children.get(i).removeParent(r);
//								System.out.println("Found Sibling of "+r.children.get(j)+": "+r.children.get(i));
//							}
//					}
//					
//				}
//			}
//			r.children = copy;
//		}
//		long end = System.currentTimeMillis();
//		log+="\nRemoving redundant siblings took "+(end-start)+" ms = "+((end-start)/1000) +" s";
//	}
//	
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
    	if(!rules.isEmpty())
    		return rules;
    	else if(!ruleMap.isEmpty()) {
    			rules = new ArrayList<IndexRule>(ruleMap.size());
    			for(Entry<Integer, HashMap<Integer, IndexRule>> e : ruleMap.entrySet()) {
    				rules.addAll(e.getValue().values());
    			}
    	}
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
    	rules.addAll(subjectToRule.get(it.next())); // init
    	returnSet.addAll(rules); // need intermediate Set.  
    	while(it.hasNext() && !rules.isEmpty()) {
    		Set<IndexRule> others = subjectToRule.get(it.next());
    		for(IndexRule ri : rules) {
    			if(!others.contains(ri))
    				returnSet.remove(ri);
    		}
    		rules.clear();
    		rules.addAll(returnSet);
//    		rules = returnSet;
    	}
//    	System.out.println("\t org" + returnSet);
    	returnSet.remove(r); // avoid transitivity
//    	System.out.println("\t red" + returnSet);
    	return returnSet;
    }
    
    /**
     * Compute rules that are
     * @param r
     * @return
     */
    private HashMap<Integer, RuleToDeleteGraph> computeDeleteBasedRules(IndexRule r, int border)  {
    	HashMap<Integer, RuleToDeleteGraph> rules = new HashMap<Integer, RuleToDeleteGraph>();
    	HashSet<Integer> rulesBeneathThreshold = new HashSet();

//		System.out.println("Computing delte rules for "+r);
//    	Set<RuleToDeleteGraph> rules = new HashSet<>();
  
//    	System.out.println("Border: "+border);
//    	System.out.println(border);
    	Set<Integer> subs = r.profile.subjects;

//    	if(subs.size()<=1)
//    		System.out.println("border of triple with "+subs.size()+" is: "+border);
    	if(subs.isEmpty())
    		return rules;
    	//ratio parent: min. 50 % subjects
    	Iterator<Integer> it = subs.iterator();
		HashSet<Integer> toDelete = new HashSet();
		HashSet<Integer> subsAlready = new HashSet();
    	while(it.hasNext()) {
    		
    		Integer s = it.next();
    		subsAlready.add(s);
    		Set<IndexRule> others = subjectToRule.get(s); // rules this s is part of
//    		System.out.println("Looking add subject "+s);
    
    		for(Entry<Integer, RuleToDeleteGraph> rTD : rules.entrySet()) { // for every possible rule
    			// if rule hasNotSubject
    			if(!rTD.getValue().parent.profile.subjects.contains(s)) {
    				if(rTD.getValue().notIn.size()>border) {
    					toDelete.add(rTD.getKey());
//    					System.out.println("to delete: "+rTD.getValue().parent);
    				}
    				// add subject to delete
    				rTD.getValue().notIn.add(s);
    			}
    		}
    		for(Integer del : toDelete) {
    			
    			rulesBeneathThreshold.add(del);
    			rules.remove(del);
//    			System.out.println("\t deleteRule due to threshold" + del);
    		}    		
    		for(IndexRule ri : others) {
//    			System.out.println("\t plays role in "+ri);
//    			RuleToDeleteGraph ruleDelete = new RuleToDeleteGraph(ri);
    			if(!rules.containsKey(ri.nr)  && !ri.equals(r)) { // fill RuleToDelete set
//    				if(subsAlready.size()<=border) {
    					HashSet<Integer> allNr = new HashSet<Integer> ();
    					for(Integer nr : subsAlready)
    						if(!ri.profile.subjects.contains(nr))
    							allNr.add(nr);
    					rules.put(ri.nr, new RuleToDeleteGraph(ri, allNr));
//    					System.out.println("\t adding possible delete rule: "+ri);
//    				}
    				
    			}
    		}
    	}    	
//    	System.out.println("Computing delete rules for "+r+"Resultts:");
//    	for(Entry<Integer, RuleToDeleteGraph> rtd:rules.entrySet()) {
//    		if(!rulesBeneathThreshold.contains(rtd.getValue().parent.nr))
//    			System.out.println(rtd.getKey()+": " + rtd);
//    	}
    	return rules;
    }
    
//    /**
//     * Returns List of Rules, atoms at last position.
//     * @return
//     */
//	public List<IndexRule> getResortedRules() {
//		List<IndexRule> copy = new LinkedList<IndexRule>();
//		copy.addAll(rules);
//		Collections.sort(copy, new IndexRuleAtomsCompartor());
//		for(int i = 0; i<copy.size(); i++) {
//			copy.get(i).atomNr = i;
//			newToOld.put(i, copy.get(i).nr);
//		}
//		return copy;
//	}
}



class RuleToDeleteGraph {
	IndexRule parent;
	HashSet<Integer> notIn = new HashSet();
	
	public RuleToDeleteGraph(IndexRule r) {
		this.parent = r;
	}
	
	public RuleToDeleteGraph(IndexRule r, HashSet<Integer> notIn) {
		this.parent = r;
		this.notIn = notIn;
	}
	
	@Override
	public int hashCode() {
		return parent.hashCode();
	}
	@Override
	public boolean equals(Object other) {
		RuleToDeleteGraph o = (RuleToDeleteGraph) other;
		return this.parent.equals(o.parent);
	}
	@Override
	public String toString() {
		return parent.toString() +" delete: "+notIn.toString();
	}
	
}
 