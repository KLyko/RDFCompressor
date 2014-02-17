package de.uni_leipzig.simba.data;

import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author Klaus Lyko
 *
 */
public class DefaultCompressedGraph implements CompressedGraph {
	List<Rule> rules;
	private Graph addGraph;
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

	
}
