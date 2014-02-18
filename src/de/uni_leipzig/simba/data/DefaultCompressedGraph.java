package de.uni_leipzig.simba.data;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Default implementation of the CompressedGraph.
 * @author Klaus Lyko
 *
 */
public class DefaultCompressedGraph implements CompressedGraph {
	/**redundant for now*/
	List<Rule> rules;
	HashSet<Rule> ruleHash;
	static Logger logger = Logger.getLogger(DefaultCompressedGraph.class);
	
	private Model model;
	public DefaultCompressedGraph() {
		rules = new LinkedList<Rule>();
		ruleHash = new HashSet<Rule>();
		model = ModelFactory.createDefaultModel();
	}
	
	public Graph getAddGraph() {
		return model.getGraph();
	}
	
	public void setAddModel(Model model) {
		this.model = model;
	}
	
	public void addRule(Rule r) {
		if(!ruleHash.contains(r)) {
//			System.out.println("Adding non redundant rule at "+rules.size());
			r.nr = rules.size();
			rules.add(r);
			boolean add=ruleHash.add(r);
//			System.out.println("Adding Rule r to hash?"+add);
		} else {
			logger.info("Not adding redundant rule");
			int nr = -1; Rule o;
			Iterator<Rule> it = ruleHash.iterator();
			while(it.hasNext()) {
				o = it.next();
				if(o.equals(r)) {
					nr = o.nr;
					o.profile.subjects.addAll(r.profile.subjects);
					rules.set(nr, o);
				}
			}
			if(nr == -1) {
				System.out.println("Error adding rules");
			}
		}
	}
	

//	public Rule findRule(Profile p) {
//		for(Rule r : rules) {
//			if(r.profile.equals(p))
//				return r;
//		}
//		return null;
//	}

	/**
	 * Finds all (different) superrules of Rule r. These are those who contain all uris of rule r.
	 * @param r
	 * @return Set of all super rules of Rule r
	 */
	public Set<Rule> getSuperRules(Rule r) {
		HashSet<Rule> result = new HashSet<Rule>();
		Collections.sort(rules);
		for(Rule o : rules) {
			if(o.profile.size()<r.profile.size())
				continue;
			else {// other has almost as many elements
				if(!r.profile.equals(o.profile) && // isn't the same
						o.profile.subjects.containsAll(r.profile.subjects)) { // other contains all uris of r
					result.add(o);
				}				
			}
		}
		return result;
	}


	@Override
	public void computeSuperRules() {
		//TODO is this really benefficial?
		Collections.sort(rules);
		//1st compute all supersets
		for(Rule r : rules) {
			Set<Rule> supersets = getSuperRules(r);
			r.parents.addAll(supersets);
		}
		//2nd remove redundant uris in supersets
		for(Rule r : rules) {
			for(Rule superRule : r.parents) {
				superRule.profile.subjects.removeAll(r.profile.subjects);
			}
		}
	}
	
	public int size() {
		int s=0;
		for(Rule r : rules) {
			s+= r.profile.size();
		}
		return s;
	}
	
}
