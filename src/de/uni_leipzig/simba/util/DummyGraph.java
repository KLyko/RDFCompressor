package de.uni_leipzig.simba.util;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.uni_leipzig.simba.data.CompressedGraph;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
public class DummyGraph {
	
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	public static IndexCompressedGraph createDummyDataCompression() throws Exception {
//		Model model = ModelFactory.createDefaultModel();
//		
//		//subjects
//		Resource s1 = model.createResource("s1");//1
//		Resource s2 = model.createResource("s2");
//		Resource s3 = model.createResource("s3");
//		Resource s4 = model.createResource("s4");
//		Resource s5 = model.createResource("s5");
//		Resource s6 = model.createResource("s6");
//		// properties (shared)
//		Property p1 = model.createProperty("p1");//1
//		Property p2 = model.createProperty("p2");//2
//		Property is_a = model.createProperty("is_a");//3
//		// objects	(shared)	
//		Resource o1 = model.createResource("o1");//7
//		Literal o2 = model.createLiteral("o2");//8
//		Resource cA = model.createResource("A");//9
//		Resource cB = model.createResource("B");//10

		//		profile 
		IndexProfile prof1 = new IndexProfile(1, 7);
		IndexRule rule1 = new IndexRule(prof1);
//		prof1.addSubject(1);
//		prof1.addSubject(2);
//		prof1.addSubject(3);
//		prof1.addSubject(4);
//		prof1.addSubject(5);		
//		
		IndexProfile prof2 = new IndexProfile(2, 8);
		IndexRule rule2 = new IndexRule(prof2);
//		prof2.addSubject(2);
//		prof2.addSubject(4);
//		prof2.addSubject(5);
		
		IndexProfile sub1 = new IndexProfile(3, 9);
		IndexRule rule3 = new IndexRule(sub1);
//		sub1.addSubject(1);
//		sub1.addSubject(2);
//		sub1.addSubject(3);
//		sub1.addSubject(4);
		
		IndexProfile sub2 = new IndexProfile(3, 10);
		IndexRule rule4 = new IndexRule(sub2);
//		sub2.addSubject(5);
//		sub2.addSubject(6);

		IndexCompressedGraph graph = new IndexCompressedGraph(10, true, 1);
		for(int i = 1; i<=5;i++) {
			graph.addRule(rule1, i);
			if(i==2 || i==4 || i==5) {
				graph.addRule(rule2, i);
			}
			if(i<5)
				graph.addRule(rule3, i);
		}
		graph.addRule(rule4, 5);
		graph.addRule(rule4, 6);
		return graph;
		
	}
	
	public static void main(String args[]) {
		
	}
}
