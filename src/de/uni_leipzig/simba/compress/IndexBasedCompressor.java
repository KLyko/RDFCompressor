package de.uni_leipzig.simba.compress;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import de.uni_leipzig.simba.data.IRule;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
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
	HashMap<Integer, String> propertyMap = new HashMap();

	public IndexBasedCompressor() {
		//nothing to do here so far.
	}
	
	 public void compress(File input) {
		 String log = "";
		 	long start = System.currentTimeMillis();
			Model model = FileManager.get().loadModel( input.toString() );
		
//			StringWriter graphOutput = new StringWriter();
//			model.write(graphOutput, "TURTLE");
//			System.out.println(graphOutput);
			
			shortToUri.putAll(model.getNsPrefixMap());
			
			// build inverse list of p/o tuples
			IndexCompressedGraph dcg = new IndexCompressedGraph();
		
			StmtIterator iter = model.listStatements();
			long middle = System.currentTimeMillis();
			long middle2 = System.currentTimeMillis();
			String print = "Loading model took: " + (middle-start) + " milli seconds";
			System.out.println(print);
			log += print +"\n";
			
			while( iter.hasNext() ){
				Statement stmt = iter.next();
				
				String s = stmt.getSubject().toString();
				try{
					s = model.shortForm(s);
				} catch(NullPointerException npe){ /*bnode*/ }
				
				String p = stmt.getPredicate().getURI();
				try{
					p = model.shortForm(p);
				} catch(NullPointerException npe){ /*bnode*/ }
				
				String o = stmt.getObject().toString();
				try{
					o = model.shortForm(o);
				} catch(NullPointerException npe){ /*bnode*/ }
				
//				System.out.println(s + " -- " + p + " -- " + o);
				int indexS = addIndex(s, SPO.SUBJECT);
				int indexP = addIndex(p, SPO.PREDICATE);
				int indexO = addIndex(o, SPO.OBJECT);
				IndexProfile profile = new IndexProfile(indexP, indexO);
				profile.addSubject(indexS);
				IndexRule rule = new IndexRule(profile);
				dcg.addRule(rule);
				
			}
			print = "Reading all rules: " + (System.currentTimeMillis()-middle) + " milli seconds =" + (System.currentTimeMillis()-middle)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			middle = System.currentTimeMillis();
			dcg.computeSuperRules();
			print = "RComputing super rules: " + (System.currentTimeMillis()-middle) + " milli seconds =" + (System.currentTimeMillis()-middle)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			middle = System.currentTimeMillis();
			dcg.removeRedundantParentRules();
			print = "Removing redundancies: : " + (System.currentTimeMillis()-middle) + " milli seconds =" + (System.currentTimeMillis()-middle)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
//			System.out.println("\nCompressed graph:\n"+dcg);
			middle = System.currentTimeMillis();

			try{

			    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			    for(IndexRule rule : dcg.getRules()) {
  			        IndexProfile profile = rule.getProfile();
				outputStream.write(Integer.toString(rule.getNumber()).getBytes());
				outputStream.write(":".getBytes());
				outputStream.write(profile.getProperty().toString().getBytes());
				outputStream.write("|".getBytes());
				outputStream.write(profile.getObject().toString().getBytes());
				outputStream.write("[".getBytes());
				Iterator ruleIter = profile.getSubjects().iterator();
				List<Integer> subjects = new LinkedList();
				subjects.addAll(profile.getSubjects());
				Collections.sort(subjects);
				int offset = 0;
				for(int i=0; i<subjects.size();i++) {
					int val = subjects.get(i);
					outputStream.write(Integer.toString(val-offset).getBytes());
					offset = val;
					if (i<subjects.size()-1){
					    outputStream.write("|".getBytes());
					}
				}
				outputStream.write("]".getBytes());
				outputStream.write("{".getBytes());
				offset = 0;
				ruleIter = rule.getParents().iterator();
				while (ruleIter.hasNext()){
				    IRule sr = (IRule) ruleIter.next();
				    outputStream.write(Integer.toString(sr.getNumber()-offset).getBytes());
				    offset= sr.getNumber();
				    if (ruleIter.hasNext()){
					outputStream.write("|".getBytes());
				    }
				}
				outputStream.write("}\n".getBytes());
			    }
			    byte rules[] = outputStream.toByteArray( );
			
			    middle2 = System.currentTimeMillis();
			
			    OutputStream os = new FileOutputStream(input.getAbsolutePath() + ".tar.bz2");
			    OutputStream bzos = new BZip2CompressorOutputStream(os);
			    TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);

			    // write prefixes
			    outputStream = new ByteArrayOutputStream( );

			    for (Entry<String, String>  entry : model.getNsPrefixMap().entrySet()) {
			    	outputStream.write( entry.getKey().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( entry.getValue().getBytes());
			    	outputStream.write( "\n".getBytes());
			    }
			    byte prefixes[] = outputStream.toByteArray( );

			    TarArchiveEntry entry = new TarArchiveEntry("prefixes");
			    entry.setSize(prefixes.length);
			    aos.putArchiveEntry(entry);
			    aos.write(prefixes);
			    aos.closeArchiveEntry();

			    // write subject index
			    outputStream = new ByteArrayOutputStream( );

			    for (Entry<Integer, String> subject : this.subjectMap.entrySet()) {
			    	outputStream.write( subject.getKey().toString().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( subject.getValue().getBytes());
			    	outputStream.write( "\n".getBytes());
			    }
			    byte subjects[] = outputStream.toByteArray( );

			    entry = new TarArchiveEntry("subjects");
			    entry.setSize(subjects.length);
			    aos.putArchiveEntry(entry);
			    aos.write(subjects);
			    aos.closeArchiveEntry();

			    // write object index
			    outputStream = new ByteArrayOutputStream( );

			    for (Entry<Integer, String> object : this.objectMap.entrySet()) {
			    	outputStream.write( object.getKey().toString().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( object.getValue().getBytes());
			    	outputStream.write( "\n".getBytes());
			    }
			    byte objects[] = outputStream.toByteArray( );

			    entry = new TarArchiveEntry("objects");
			    entry.setSize(objects.length);
			    aos.putArchiveEntry(entry);
			    aos.write(objects);
			    aos.closeArchiveEntry();

			    // write property index
			    outputStream = new ByteArrayOutputStream( );

			    for (Entry<Integer, String> property : this.propertyMap.entrySet()) {
			    	outputStream.write( property.getKey().toString().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( property.getValue().getBytes());
			    	outputStream.write( "\n".getBytes());
			    }
			    byte properties[] = outputStream.toByteArray( );
			    
			    entry = new TarArchiveEntry("properties");
			    entry.setSize(properties.length);
			    aos.putArchiveEntry(entry);
			    aos.write(properties);
			    aos.closeArchiveEntry();

			    // write rules
			    entry = new TarArchiveEntry("rules");
			    entry.setSize(rules.length);
			    aos.putArchiveEntry(entry);
			    aos.write(rules);
			    aos.closeArchiveEntry();
			    
			    aos.finish();
			    aos.close();
			    bzos.close();
			    os.close();
			}
			catch (IOException ioe){
				System.out.println(ioe);
			}
			print = "Serializing Rulestring: : " + (middle2-middle) + " milli seconds =" + (middle2-middle)/1000 +" seconds";
			print += "\nWriting files: : " + (System.currentTimeMillis()-middle2) + " milli seconds =" + (System.currentTimeMillis()-middle2)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			print = "Overall : " + (System.currentTimeMillis()-start) + " milli seconds =" + (System.currentTimeMillis()-start)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			writeLogFile(input, log);
//			System.out.println(ruleString);
		}
	
	
	private void writeLogFile(File source, String log) {
		File logFile = new File("source.getAbsolutePath()+_log.txt");
		try {
			
			FileWriter writer =  new FileWriter(logFile, false);
			writer.write(log);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
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
				if(propertyMap.containsValue(uri)) {
					for(Entry<Integer, String> e:propertyMap.entrySet())
						if(e.getValue().equals(uri)) {
							index = e.getKey();
							break;
						}	
				} else { // create new one
					index = propertyMap.size();
					propertyMap.put(propertyMap.size(), uri);
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
