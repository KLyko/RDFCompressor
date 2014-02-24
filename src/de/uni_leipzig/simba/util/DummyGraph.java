package de.uni_leipzig.simba.util;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.uni_leipzig.simba.data.CompressedGraph;
import de.uni_leipzig.simba.data.DefaultCompressedGraph;
import de.uni_leipzig.simba.data.Profile;
import de.uni_leipzig.simba.data.Rule;

public class DummyGraph {
	
	/**
	 * 
	 * @return
	 */
	public static DefaultCompressedGraph createDummyDataCompression() {
		Model model = ModelFactory.createDefaultModel();
//		
		//subjects
		Resource s1 = model.createResource("s1");
		Resource s2 = model.createResource("s2");
		Resource s3 = model.createResource("s3");
		Resource s4 = model.createResource("s4");
		Resource s5 = model.createResource("s5");
		Resource s6 = model.createResource("s6");
		// properties (shared)
		Property p1 = model.createProperty("p1");
		Property p2 = model.createProperty("p2");
		Property is_a = model.createProperty("is_a");
		// objects	(shared)	
		Resource o1 = model.createResource("o1");
		Literal o2 = model.createLiteral("o2");
		Resource cA = model.createResource("A");
		Resource cB = model.createResource("B");

	
//		profile 
		Profile prof1 = new Profile(p1, o1);
		prof1.addSubject(s1);
		prof1.addSubject(s2);
		prof1.addSubject(s3);
		prof1.addSubject(s4);
		prof1.addSubject(s5);		
		
		Profile prof2 = new Profile(p2, o2);
		prof2.addSubject(s2);
		prof2.addSubject(s4);
		prof2.addSubject(s5);
		
		Profile sub1 = new Profile(is_a, cA);
		sub1.addSubject(s1);
		sub1.addSubject(s2);
		sub1.addSubject(s3);
		sub1.addSubject(s4);
//		sub1.addSubject(s5);
//		sub1.addSubject(s6);
		
		Profile sub2 = new Profile(is_a, cB);
		sub1.addSubject(s5);
		sub1.addSubject(s6);
		
//		Rules
		Rule rule1 = new Rule(prof1);
		Rule rule2 = new Rule(prof2);
		Rule subRule1 = new Rule(sub1);
		Rule subRule2 = new Rule(sub2);
		subRule2.addParent(subRule1);
		
		DefaultCompressedGraph graph = new DefaultCompressedGraph();
		graph.addRule(rule1);
		graph.addRule(rule2);
		graph.addRule(subRule1);
		graph.addRule(subRule2);
		
		// AddGraph
		Model addModel = ModelFactory.createDefaultModel();
	

		Resource o12 = addModel.createResource("o12");
		Resource o13 = addModel.createResource("o13");
		Property p12 = addModel.createProperty("p12");
		Property p13 = addModel.createProperty("p13");
		s1.addProperty(p12, o12);
		s1.addProperty(p13, o13);
		
		 s3 = addModel.createResource("s3");
		Property p32 = addModel.createProperty("p32");
		s3.addProperty( p32, "o32");
		

		 s6 = addModel.createResource("s6");
		Property p61 = addModel.createProperty("p61");
		s6.addProperty(p61, "o61");
		
		graph.setAddModel(addModel);
		
		
		return graph;
		
	}
	
	public static void main(String args[]) {
		
	}
}
