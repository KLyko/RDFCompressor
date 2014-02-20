package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import de.uni_leipzig.simba.data.DefaultCompressedGraph;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.data.Profile;
import de.uni_leipzig.simba.data.Rule;
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

	 public void compress(File input) {
			Model model = FileManager.get().loadModel( input.toString() );
		
			StringWriter graphOutput = new StringWriter();
			model.write(graphOutput, "TURTLE");
			System.out.println(graphOutput);
		
			// build inverse list of p/o tuples
			IndexCompressedGraph dcg = new IndexCompressedGraph();
		
			StmtIterator iter = model.listStatements();
			while( iter.hasNext() ){
			    Statement stmt = iter.next();
			    IndexProfile profile = new IndexProfile(stmt.getPredicate(),
							  stmt.getObject());
			    profile.addSubject(stmt.getSubject());
			    IndexRule rule = new IndexRule(profile);
		
			    dcg.addRule(rule);
			}
			System.out.println("\nCompressed graph:\n"+dcg);
			
			// (build addgraph)
		
			// serialize inverse list
			String output = dcg.serialize();
			System.out.println("Serialized compressed graph:\n" + output);
		
			// compress with bzip2
			try{
			    OutputStream os = new FileOutputStream(input.getAbsolutePath() + ".bz2");
			    OutputStream bzos = new BZip2CompressorOutputStream(os);
			    bzos.write(output.getBytes());
			    bzos.close();
			    os.close();
			}
			catch (IOException ioe){
			    System.out.println(ioe);
			}
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
