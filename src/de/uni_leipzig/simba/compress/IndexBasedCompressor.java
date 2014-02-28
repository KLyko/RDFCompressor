package de.uni_leipzig.simba.compress;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
	String log = "One HashMap for object and subject\n";
	HashMap<String, String> shortToUri = new HashMap();
	HashMap<String, Integer> subjectMap = new HashMap();
//	HashMap<String, Integer> objectMap = new HashMap();
	HashMap<String, Integer> propertyMap = new HashMap();
	public IndexBasedCompressor() {
		//nothing to do here so far.
	}
	
	 public void compress(File input) {
		 	log += input.getAbsolutePath()+"\n";
		 	
			long byteLength = input.length();
			log+= "Length in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB\n\n";
 			
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
			String print = "Loading model took: " + (middle-start) + " milli seconds = "+ (middle-start) /1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			int stmtCount = 0;
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
				stmtCount++;
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
			middle = System.currentTimeMillis();

			try{

			    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			    for(IndexRule rule : dcg.getRules()) {
	  			        IndexProfile profile = rule.getProfile();
					//outputStream.write(Integer.toString(rule.getNumber()).getBytes());
					//outputStream.write(":".getBytes());
					outputStream.write(profile.getProperty().toString().getBytes());
					outputStream.write("|".getBytes());
					outputStream.write(profile.getObject().toString().getBytes());
					Iterator ruleIter = profile.getSubjects().iterator();
					int offset = 0;
					
					if(profile.size()>0) {
						outputStream.write("[".getBytes());
				
						List<Integer> subjects = new LinkedList();
						subjects.addAll(profile.getSubjects());
						Collections.sort(subjects);
					
						for(int i=0; i<subjects.size();i++) {
							int val = subjects.get(i);
							outputStream.write(Integer.toString(val-offset).getBytes());
							offset = val;
							if (i<subjects.size()-1){
							    outputStream.write("|".getBytes());
							}
						}// for each subject
					}// if rule has subjects
					if(rule.getParents().size()>0) {
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
						}// for each parent
					}// if rule has parents
					outputStream.write("\n".getBytes());
			    }// foreach rule
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

			    for (Entry<String, Integer> subject : this.subjectMap.entrySet()) {
			    	outputStream.write( subject.getKey().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( subject.getValue().toString().getBytes());
			    	outputStream.write( "\n".getBytes());
			    }
			    byte subjects[] = outputStream.toByteArray( );

			    entry = new TarArchiveEntry("subjects");
			    entry.setSize(subjects.length);
			    aos.putArchiveEntry(entry);
			    aos.write(subjects);
			    aos.closeArchiveEntry();

//			    // write object index
//			    outputStream = new ByteArrayOutputStream( );
//
//			    for (Entry<String, Integer> object : this.objectMap.entrySet()) {
//			    	outputStream.write( object.getKey().getBytes());
//			    	outputStream.write( "|".getBytes());
//			    	outputStream.write( object.getValue().toString().getBytes());
//			    	outputStream.write( "\n".getBytes());
//			    }
//			    byte objects[] = outputStream.toByteArray( );
//
//			    entry = new TarArchiveEntry("objects");
//			    entry.setSize(objects.length);
//			    aos.putArchiveEntry(entry);
//			    aos.write(objects);
//			    aos.closeArchiveEntry();

			    // write property index
			    outputStream = new ByteArrayOutputStream( );

			    for (Entry<String, Integer> property : this.propertyMap.entrySet()) {
			    	outputStream.write( property.getKey().getBytes());
			    	outputStream.write( "|".getBytes());
			    	outputStream.write( property.getValue().toString().getBytes());
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
				log += "\nExeption:"+ioe+" \n";
			}
			print = "Serializing Rulestring: : " + (middle2-middle) + " milli seconds =" + (middle2-middle)/1000 +" seconds";
			print += "\nWriting files: : " + (System.currentTimeMillis()-middle2) + " milli seconds =" + (System.currentTimeMillis()-middle2)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n";
			print = "Overall : " + (System.currentTimeMillis()-start) + " milli seconds =" + (System.currentTimeMillis()-start)/1000 +" seconds";
			System.out.println(print);
			log += print +"\n\n";
			File outFile = new File(input.getAbsolutePath() + ".tar.bz2");
			byteLength = outFile.length();
			
			int nrOfRules = dcg.getRules().size();
			int sizeOfRules = dcg.size();
			double tripleRatio = new Double(sizeOfRules)/new Double(stmtCount);
			log +="Nr of triples="+stmtCount+" Nr of Rules="+nrOfRules+" Size of Rules="+sizeOfRules+" ratio(#triples/Rule.size())="+tripleRatio;
			
			log+= "Length in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB";
			long n3 = computePlainNTripleBZ2Size(model, input);
			log += "\n";
			double sizeRatio =  new Double(byteLength) / new Double(n3);
			log += "Orginal N3 length in Byte = "+n3+" = "+n3/1024+" KB ="+n3/(1024*1024)+" MB Ratio Our/BZ2="+sizeRatio;
 			writeLogFile(input, log);

 		
		 
//			printDebug(dcg);
		}
	
	 
	 public long computePlainNTripleBZ2Size(Model model, File orgFile) {
		 long size = 0;				
		 File out = new File(orgFile.getAbsolutePath()+"_N3.n3.bz2");
		 if(!out.exists())
		   try {

			   
			   	ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
				model.write(outputStream, "N3-TRIPLE");

			   
				OutputStream os = new FileOutputStream(orgFile.getAbsolutePath()+"_N3.n3.bz2");
			    OutputStream bzos = new BZip2CompressorOutputStream(os);
			    TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);

			    byte n3s[] = outputStream.toByteArray( );

			    TarArchiveEntry entry = new TarArchiveEntry(orgFile.getAbsolutePath()+"_N3.n3");
			    entry.setSize(n3s.length);
			    aos.putArchiveEntry(entry);
			    aos.write(n3s);
			    aos.closeArchiveEntry();
			    
			    aos.finish();
			    aos.close();
			    bzos.close();
			    os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			size = out.length();
			return size;
	 }
	
	private void writeLogFile(File source, String log) {
		File logFile = new File(source.getAbsolutePath()+"_log.txt");
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
				if(subjectMap.containsKey(uri)) {
					index = subjectMap.get(uri);
							break;
				}	
				else { // create new one
					index = subjectMap.size();
					subjectMap.put(uri, index);
				}
				break;
			case PREDICATE: 
				if(propertyMap.containsKey(uri)) {
					index = propertyMap.get(uri);
							break;
				}	
				else { // create new one
					index = propertyMap.size();
					propertyMap.put(uri, propertyMap.size());
				}				
				break;
			case OBJECT:
//				if(objectMap.containsKey(uri)) {
//					index = objectMap.get(uri);
//					break;
//				} else { // check for subjects
//					if(subjectMap.containsKey(uri)) {
//						uri = ""+subjectMap.get(uri);
//					}
//					if(objectMap.containsKey(uri)) {
//						index = objectMap.get(uri);
//						break;
//					}else {
//						index = objectMap.size();
//						objectMap.put(uri, index);
//					}
//				}		
				index = addIndex(uri, SPO.SUBJECT);
				break;
		}
		return index;
	}
	
	public void printDebug(IndexCompressedGraph graph) {
//		System.out.println("\nCompressed graph:\n"+graph.toString());
//		System.out.println("Subjects:\n"+subjectMap);
//
//		System.out.println("Predicates:\n"+propertyMap);
//		
//		System.out.println("Objects:\n"+objectMap);
		System.out.println("GRAPH...\n");
		for(IndexRule rule: graph.getRules()) {
			String out= ""+rule.getNumber()+": ";
			out+=getUri(rule.getProfile().getProperty(), SPO.PREDICATE)+" - "+getUri(rule.getProfile().getObject(), SPO.OBJECT);
			out+="[";
			Iterator<Integer> subjectIter = rule.getProfile().getSubjects().iterator();
			while(subjectIter.hasNext()) {
				out += getUri(subjectIter.next(), SPO.SUBJECT);
				if(subjectIter.hasNext())
					out += ", ";
			}
			out += "]";
			if(rule.getParents().size()>0) {
				out += " {";
				Iterator<IRule<IndexProfile>> it = rule.getParents().iterator();
				while(it.hasNext()) {
					out+= it.next().getNumber();
					if(it.hasNext())
						out += ", ";
				}
				out += "}";
				
			}
			System.out.println(out);			
		}
	}
	
	public String getUri(Integer index, SPO SPOrO) {
		String uri = "NOT_FOUND";
		switch(SPOrO) {
		case SUBJECT:
			if(subjectMap.containsValue(index)) {
				for(Entry<String, Integer> e : subjectMap.entrySet()) {
					if(e.getValue() == index) {
						uri = e.getKey();
						break;
					}
				}
			}
			break;
		case PREDICATE:
			if(propertyMap.containsValue(index)) {
				for(Entry<String, Integer> e : propertyMap.entrySet()) {
					if(e.getValue() == index) {
						uri = e.getKey();
						break;
					}
				}
			}
			break;
		case OBJECT:
//			if(objectMap.containsValue(index)) {
//				for(Entry<String, Integer> e : objectMap.entrySet()) {
//					if(e.getValue() == index) {
//						uri = e.getKey();
//						try { 
//							int subjectIndex = Integer.parseInt(uri);
//							uri = getUri(subjectIndex, SPO.SUBJECT);
//						}catch(NumberFormatException nfe){}
//						break;
//					}
//				}
//			}
			uri = getUri(index, SPO.SUBJECT);
			break;
		}
		return uri;
	}
}
