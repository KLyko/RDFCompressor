package de.uni_leipzig.simba.data;



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
	 * Method to compute all supersets of each rule, add pointers to them and remove redundant uris of the
	 * subsets in the supersets.
	 * @TODO is there an more time-wise efficient way of computing while generating the proiles?
	 */
	public void computeRedundantRules();
}
