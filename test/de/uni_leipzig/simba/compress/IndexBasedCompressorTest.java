package de.uni_leipzig.simba.compress;

import static org.junit.Assert.*;

import org.junit.Test;

public class IndexBasedCompressorTest {

	@Test
	public void testIndexex() {
		IndexBasedCompressor comp = new IndexBasedCompressor();
		comp.addIndex("s1", SPO.SUBJECT);
		comp.addIndex("s1", SPO.SUBJECT);
		comp.addIndex("s2", SPO.SUBJECT);
		comp.addIndex("p1", SPO.PREDICATE);
		comp.addIndex("p2", SPO.PREDICATE);
		comp.addIndex("p1", SPO.PREDICATE);
		comp.addIndex("o1", SPO.OBJECT);
		comp.addIndex("s1", SPO.OBJECT);
		comp.addIndex("s1", SPO.OBJECT);
		assertTrue(comp.objectMap.size() == 2);
		assertTrue(comp.predicateMap.size() == 2);
		assertTrue(comp.subjectMap.size() == 2);
	}

}
