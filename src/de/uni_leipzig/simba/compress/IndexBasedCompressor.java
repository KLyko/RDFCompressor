package de.uni_leipzig.simba.compress;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
/**
 * Implementation of an index based compression:
 * Creates Indexes for all subjects, objects, predicates. And operates on them
 * @author Klaus Lyko
 *
 */
public class IndexBasedCompressor implements Compressor, IndexBasedCompressorInterface {
	
	HashMap<String, String> shortToUri = new HashMap();
	HashMap<Integer, String> subjectMap = new HashMap();
	HashMap<Integer, String> objectMap = new HashMap();
	HashMap<Integer, String> predicateMap = new HashMap();
	
	public IndexBasedCompressor() {
		//nothing to do here so far.
	}
	

	@Override
	public void writeIndexFiles() {
		// TODO Auto-generated method stub
		System.out.println("short-to-uri\n====="+shortToUri);
		System.out.println("subjectMap\n====="+subjectMap);
		System.out.println("predicateMap\n====="+predicateMap);
		System.out.println("objectMap\n====="+objectMap);
	}

	@Override
	public void compress(File input) {
		// TODO Auto-generated method stub

	}
	
	
	@Override
	public void addAbbreviation(String sURI, String fullURI) {
		shortToUri.put(sURI, fullURI);
	}

	@Override
	public String getAbbreviation(String uri) {
		if(shortToUri.containsValue(uri))
			for(String sUri:shortToUri.keySet())
				if(shortToUri.get(sUri).equals(uri))
					return sUri;
		return uri;				
	}

	@Override
	public String getFullUri(String sUri) {
		if(shortToUri.containsKey(sUri))
			return shortToUri.get(sUri);
		return sUri;
	}

	@Override
	public int addIndex(String uri, SPO SPOrO) {
		int index =-1;
		switch(SPOrO) {
			case SUBJECT: 
				if(subjectMap.containsValue(uri)) {
					for(Entry<Integer, String> e:subjectMap.entrySet())
						if(e.getValue().equals(uri)) {
							index = e.getKey();
							break;
						}	
				} else { // create new one
					index = subjectMap.size();
					subjectMap.put(subjectMap.size(), uri);
				}				
				break;
			case PREDICATE: 
				if(predicateMap.containsValue(uri)) {
					for(Entry<Integer, String> e:predicateMap.entrySet())
						if(e.getValue().equals(uri)) {
							index = e.getKey();
							break;
						}	
				} else { // create new one
					index = predicateMap.size();
					predicateMap.put(predicateMap.size(), uri);
				}				
				break;
			case OBJECT:
				if(objectMap.containsValue(uri)) {
					for(Entry<Integer, String> e:objectMap.entrySet())
						if(e.getValue().equals(uri)) {
							index = e.getKey();
							break;
						}	
				} else { // check for subjects
					if(subjectMap.containsValue(uri)) {
						int subjectIndex = -1;
						for(Entry<Integer, String> e:subjectMap.entrySet())
							if(e.getValue().equals(uri)) {
								subjectIndex = e.getKey();
								break;
							}
						if(objectMap.containsValue(""+subjectIndex)) {
							for(Entry<Integer, String> e:objectMap.entrySet())
								if(e.getValue().equals(""+subjectIndex)) {
									index = e.getKey();
									break;
								}	
						} else { // create new one
							index = objectMap.size();
							objectMap.put(objectMap.size(), ""+subjectIndex);
						}				
						break;
					} //also subject doesn#T exists 
					index = objectMap.size();
					objectMap.put(objectMap.size(), uri);
				}				
				break;
		}
		return index;
	}
}
