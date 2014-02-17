package de.uni_leipzig.simba.data;


import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * implements Serializable
 * List<Rule> rules
 * Graph addGraph //JENA Graph
 * 
 * @author Klaus Lyko
 *
 */
public interface CompressedGraph {

	/**
	 * FIXME do we need this?
	 * @return
	 */
	public Graph getAddGraph();
	
	/**
	 * Setter for the addGraph, which is an RDF model holding all additional triples not compressed via Rules.
	 * @param model
	 */
	public void setAddModel(Model model);
	
	/**
	 * Add a Rule.
	 * @param r
	 */
	public void addRule(Rule r);
	
	/**
	 * Method tries to find the rule for the given profile. E.g. 
	 * @param p profile for wich we try to find a rule
	 * @return Rule for this profile, if it exists; null otherwise.
	 */
	public Rule findRule(Profile p);
	
	/**
	 * Dummy for finding super rules: rules which at least contain all subjects of Rule r.
	 * @param r
	 * @return Set of Rules which also contain all subjects of r.
	 */
	public void computeRedundantRules();
}
