package de.uni_leipzig.simba.data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;


/**
 * Default implementation of the CompressedGraph.
 * @author Klaus Lyko
 *
 */
public class IndexCompressedGraph implements CompressedGraph<IndexRule>{
	/**redundant for now*/
	List<IndexRule> rules;
	HashSet<IndexRule> ruleHash;
	HashMap<Integer, IndexRule> ruleMap;
	static Logger logger = Logger.getLogger(IndexCompressedGraph.class);
	
	public IndexCompressedGraph() {
		rules = new LinkedList<IndexRule>();
		ruleMap = new HashMap();
		ruleHash = new HashSet<IndexRule>();
	}

	
	public void addRule(IndexRule r) throws Exception {
		if(!ruleHash.contains(r)) {
			r.nr = rules.size();
//			rules.put(r);
			ruleMap.put(r.hashCode(), r);
			ruleHash.add(r);
		} else {
			IndexRule o = ruleMap.get(r.hashCode());
			if(!o.equals(r)) {
				System.err.println("Retrieved rule isn't the same");
				throw new Exception("Retrieved rule isn't the same");
			}
			o.profile.subjects.addAll(r.profile.subjects);
			
//			logger.info("Not adding redundant rule");
//			int nr = -1; IndexRule o;
//			Iterator<IndexRule> it = ruleHash.iterator();
//			while(it.hasNext()) {
//				o = it.next();
//				if(o.equals(r)) {
//					nr = o.nr;
//					o.profile.subjects.addAll(r.profile.subjects);
//					rules.set(nr, o);
//					return;
//				}
//			}
//			if(nr == -1) {
//				System.out.println("Error adding rules");
//			}
		}
	}
	

	public Set<IndexRule> getSuperRules(IndexRule r) {
		HashSet<IndexRule> result = new HashSet<IndexRule>();
		// Collections.sort(rules);
		
		for(IndexRule o : rules) {
//			IndexRule o = e.getValue();
			if(o.profile.size()<r.profile.size())
				continue;
			else {// other has almost as many elements
				if(!r.profile.equals(o.profile))// isn't the same
				    if(!r.profile.subjects.isEmpty() ) // isn't empty
						if(r.profile.min>=o.profile.min && // check uri ranges
							r.profile.max<=o.profile.max)
							if(o.profile.subjects.containsAll(r.profile.subjects) && // other contains all uris of r
									!o.parents.contains(r)) // avoid double linking to parent
							{
									result.add(o);				
							}
			}
		}
		return result;
	}

	@Override
	public void computeSuperRules() {
		rules = new ArrayList<IndexRule>(rules.size());
		rules.addAll(ruleHash);
    	Collections.sort(rules, new IndexRuleComparator());
    	for(IndexRule r : rules) 
    		r.setNumber(rules.indexOf(r));
//		Collections.sort(rules); // O(n*log n)
		//1st compute all supersets
		for(IndexRule r : rules) { //O(n²)
			if(r.getProfile().subjects.size()>1) {
				Set<IndexRule> supersets = getSuperRules(r);
				r.parents.addAll(supersets);
			}
		}
		//2nd remove redundant uris in supersets
		for(IndexRule r : rules) { //O(n)
			for(IRule<IndexProfile> superRule : r.parents) {
				superRule.getProfile().subjects.removeAll(r.profile.subjects);
			}
		}
	}

	@Override
	public void removeRedundantParentRules() {
		for(IndexRule r : rules) {
			List<IRule<IndexProfile>> copy = new LinkedList();
			copy.addAll(r.parents);
			for(IRule<IndexProfile> parent : r.parents)
				copy.removeAll(parent.getParents());
			r.parents = copy;
		}
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
   
}
