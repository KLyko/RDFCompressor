package de.uni_leipzig.simba.data;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Test class for Default implementation.
 * @author Klaus Lyko
 *
 */
public class CompressedGraphTest {
	
	@Test
	/**
	 * Tests if redundant rules are recognized
	 */
	public void testAdd() {
		Model model = ModelFactory.createDefaultModel();
		Resource s1 = model.createResource("s1");
		Resource s2 = model.createResource("s2");
		Resource s3 = model.createResource("s3");
		Resource s4 = model.createResource("s4");
		Resource s5 = model.createResource("s5");
		// p1 == p1b && o1 == o1b 
		Property p1 = model.createProperty("p1");
		Property p1b = model.createProperty("p1");
		Resource o1 = model.createResource("o1");
		Resource o1b = model.createResource("o1");

		Profile prof1 = new Profile(p1, o1);
		prof1.addSubject(s1);
		prof1.addSubject(s2);
		
		Profile prof2 = new Profile(p1, o1);
		prof2.addSubject(s3);
		prof2.addSubject(s4);
		prof2.addSubject(s5);
		
		Profile prof1a = new Profile(p1, o1);
		prof1a.addSubject(s1);
		prof1a.addSubject(s2);
		
		Profile prof1b = new Profile(p1b, o1b);
		prof1b.addSubject(s1);
		prof1b.addSubject(s2);

		Rule r1 = new Rule(prof1);
		
		Rule r2 = new Rule(prof2);
		
		DefaultCompressedGraph g = new DefaultCompressedGraph();
		g.addRule(r1);
		g.addRule(r2);
		assertTrue(	g.rules.size() == 1);
		
		g.addRule(new Rule(prof1b));
		assertTrue(	g.rules.size() == 1);
		
		assertTrue(g.rules.get(0).profile.size() == 5);
	}
	
	@Test
	/**
	 * Test for the computation of the super rules for a given rule.
	 */
	public void testGetSuperRules() {
		Model model = ModelFactory.createDefaultModel();
		Resource s1 = model.createResource("s1");
		Resource s2 = model.createResource("s2");
		Resource s3 = model.createResource("s3");
		Resource s4 = model.createResource("s4");
		Resource s5 = model.createResource("s5");
		// properties
		Property p1 = model.createProperty("p1");
		Property p2 = model.createProperty("p2");
		// objects
		Resource o1 = model.createResource("o1");
		Resource o2 = model.createResource("o2");


	
//		profile 
		Profile prof1 = new Profile(p1, o1);
		prof1.addSubject(s1);
		prof1.addSubject(s2);
		prof1.addSubject(s3);
		prof1.addSubject(s4);
		prof1.addSubject(s5);
		
		
		
		Profile prof2 = new Profile(p2, o2);
		prof2.addSubject(s3);
		prof2.addSubject(s4);
		prof2.addSubject(s5);
		
		Rule r1 = new Rule(prof1);
		
		Rule r2 = new Rule(prof2);
		
		DefaultCompressedGraph g = new DefaultCompressedGraph();
		g.addRule(r1);
		g.addRule(r2);
		

		Set<Rule> superOf1 = g.getSuperRules(r1);
		Set<Rule> superOf2 = g.getSuperRules(r2);
		
		assertTrue(superOf1.size() == 0);
		assertTrue(superOf2.size() == 1);
	}
	
	
	@Test
	/**
	 * Test for global superset computation: Finding all supe rules and avoid redundant uris.
	 */
	public void testSuperSetComputation() {
//		fail("Not implemented yet");
		Model model = ModelFactory.createDefaultModel();
		Resource s1 = model.createResource("s1");
		Resource s2 = model.createResource("s2");
		Resource s3 = model.createResource("s3");
		Resource s4 = model.createResource("s4");
		Resource s5 = model.createResource("s5");
		// properties
		Property p1 = model.createProperty("p1");
		Property p2 = model.createProperty("p2");
		Property p3 = model.createProperty("p3");
		
		// objects
		Resource o1 = model.createResource("o1");
		Resource o2 = model.createResource("o2");
		Resource o3 = model.createResource("o3");

	
//		profile 
		Profile prof1 = new Profile(p1, o1);
		prof1.addSubject(s1);
		prof1.addSubject(s2);
		prof1.addSubject(s3);
		prof1.addSubject(s4);
		prof1.addSubject(s5);
		
		Profile prof2 = new Profile(p2, o2);
		prof2.addSubject(s3);
		prof2.addSubject(s4);
		prof2.addSubject(s5);
		
		Profile prof3 = new Profile(p3, o3);
		prof3.addSubject(s4);
		prof3.addSubject(s5);
		
		Rule r1 = new Rule(prof1);		
		Rule r2 = new Rule(prof2);
		Rule r3 = new Rule(prof3);
		
		DefaultCompressedGraph g = new DefaultCompressedGraph();
		g.addRule(r1);
		g.addRule(r2);
		g.addRule(r3);
		int size0 = g.size();
		g.computeSuperRules();
		int size1 = g.size();
		System.out.println("Size before supers:" +size0+" - Size with removed supers "+size1);
		assertTrue(size1 < size0);
	}
	
}
