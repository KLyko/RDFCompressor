package de.uni_leipzig.simba.data;

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

	public Set<Rule> getSuperRules(Rule r) {
		HashSet<Rule> result = new HashSet<Rule>();
		// FIXME prototype
		return result;
	}


	@Override
	public void computeRedundantRules() {
		// TODO Auto-generated method stub		
	}
}
