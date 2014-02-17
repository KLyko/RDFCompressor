package de.uni_leipzig.simba.data;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Default implementation of the CompressedGraph.
 * @author Klaus Lyko
 *
 */
public class DefaultCompressedGraph implements CompressedGraph {
	List<Rule> rules;
//	private Graph addGraph;
	private Model model;
	public DefaultCompressedGraph() {
		rules = new LinkedList<Rule>();
		model = ModelFactory.createDefaultModel();
		
	}
	
	
	public Graph getAddGraph() {
		return model.getGraph();
	}
	
	public void setAddModel(Model model) {
		this.model = model;
	}
	
	public void addRule(Rule r) {
		rules.add(r);
	}
	

	public Rule findRule(Profile p) {
		for(Rule r : rules) {
			if(r.profile.equals(p))
				return r;
		}
		return null;
	}

	private Set<Rule> getSuperRules(Rule r) {
		HashSet<Rule> result = new HashSet<Rule>();
		for(Rule o : rules) {
			if(o.profile.size()<r.profile.size())
				continue;
			else {// other has almost as many elements
				if(o.profile.subjects.containsAll(r.profile.subjects)) {
					result.add(o);
				}				
			}
		}
		return result;
	}


	@Override
	public void computeRedundantRules() {
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
	
}
