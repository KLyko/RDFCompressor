package de.uni_leipzig.simba.compress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.itadaki.bzip2.BZip2OutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.data.IRule;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.data.SubjectCount;
import de.uni_leipzig.simba.io.ModelLoader;
/**
 * Implementation of an index based compression:
 * Creates Indexes for all subjects, objects, predicates. And operates on them
 * @author Klaus Lyko
 *
 */
public class IndexBasedCompressor implements Compressor, IndexBasedCompressorInterface {
	public String logFileSuffix="";
	int bloomErrorRate = 0;
	
	/**Used to separate elements in Lists (Subjects, Superrules)*/
	public static final String LIST_SEP = "|";
	/**Used to separate property and object index*/
	public static final String PROP_OBJ_SEP = "-";
	/**Used to separate P-O Part of a rule from its subjects*/
	public static final String PO_SUBJ_SEP = "{";
	/**Used to separate Subjectlist from Super rule list*/
	public static final String SUBJ_SUPERRULE_SEP = "[";
	/**Used to separate file parts: Prefixes, Subject dictonionary, Property dictionary, Rules*/
	public static final String FILE_SEP = "||";
	/**Used to seperate subject delete list from rest of rule*/
	public static final String DEL_SUB = "(";
	
	String log = "-->One HashMap for object and subject\n" +
			"--> sorted by props\n" +
			"--> one tar archive\n " +
			"--> sort uris frequence based";

	HashMap<String, String> shortToUri = new HashMap();
	HashMap<String, SubjectCount> subjectMap = new HashMap();
	HashMap<Integer, String> indexToSubjectMap = new HashMap();
	List<SubjectCount> resortSubjectList = new ArrayList<SubjectCount>();
	
//	HashMap<String, Integer> objectMap = new HashMap();
	/**Maps propties to their key*/
	HashMap<String, Integer> propertyMap = new HashMap();
	/**Holds the properties where the index is equivalent to the key in the map*/
	List<String> propertyList = new ArrayList<>();
	/**
	 * Maps the old subject index to the new frequence-based one.
	 * oldIndex (as in subjectMap => new resorted one);
	 */
	HashMap<Integer, Integer> subIndexMap;
	
	IndexCompressedGraph dcg; Model model;
	public IndexBasedCompressor() {
		//nothing to do here so far.
	}
	
	 public void compress(File input) {
		 	log += input.getAbsolutePath()+"\n";
		
			long byteLength = input.length();
			log+= "Length in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB\n\n";
			writeLogFile(input, log, false);
			
		 	long start = System.currentTimeMillis();
		 	try {
//			 	model = FileManager.get().loadModel( input.toString() );
		 		model = ModelLoader.getModel(input.getAbsolutePath());
	//			StringWriter graphOutput = new StringWriter();
	//			model.write(graphOutput, "TURTLE");
	//			System.out.println(graphOutput);
				
				shortToUri.putAll(model.getNsPrefixMap());
				
				// build inverse list of p/o tuples
				dcg = new IndexCompressedGraph(model.size(), true);
				
				StmtIterator iter = model.listStatements();
				
				long middle = System.currentTimeMillis();
				long middle2 = System.currentTimeMillis();
				String print = "Loading model took: " + (middle-start) + " milli seconds = "+ (middle-start) /1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
				
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
					try{
						dcg.addRule(rule, indexS);
					}catch(Exception e) {
						e.printStackTrace();
						print = "Error adding rule!";
						System.out.println(print);
						writeLogFile(input, print, true);
					}
					stmtCount++;
				}
				
				print = "Reading all rules: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
	//			log += print +"\n";
				writeLogFile(input, print, true);
				middle = System.currentTimeMillis();
				printDebug(dcg);
				dcg.computeSuperRules();
				print = "Computing super rules: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
	//			log += print +"\n";
	
				middle = System.currentTimeMillis();
				dcg.removeRedundantParentRules();
				print = "Removing redundancies: : " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
				
				System.out.println(dcg.log);
				writeLogFile(input, dcg.log, true);
				
	//			log += print +"\n";
				middle = System.currentTimeMillis();
				subIndexMap = sortFrequenceBased(subjectMap);
				long end = System.currentTimeMillis();
				print = "Sorting Frequence Based took "+(end-middle)+" ms = "+((end-middle)/1000)+ " s";
				System.out.println(print);
				writeLogFile(input, print, true);
				middle2 = System.currentTimeMillis();
				try{
					writeSingleTarFile(input);			   
				}
				catch (IOException ioe){
					File errorFile = new File(input.getAbsolutePath()+"_error.txt");
					PrintStream ps = new PrintStream(errorFile);
					System.setErr(ps);
					ioe.printStackTrace();
					ioe.printStackTrace(ps);
					log += "\nExeption:"+ioe+" \n";
					writeLogFile(input, "\nExeption:"+ioe+" \n", true);
				}
				
	//			print = "Serializing Rulestring: : " + (middle2-middle) + " milli seconds =" + (middle2-middle)/1000 +" seconds";
				print += "\nWriting files: : " + (System.currentTimeMillis()-middle2) + " milli seconds = " + (System.currentTimeMillis()-middle2)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
	//			log += print +"\n";
				print = "Overall : " + (System.currentTimeMillis()-start) + " milli seconds = " + (System.currentTimeMillis()-start)/1000 +" seconds";
				System.out.println(print);
	//			log += print +"\n\n";
				writeLogFile(input, print, true);
				File outFile = new File(input.getAbsolutePath() + ".tar.bz2");
				byteLength = outFile.length();
				
				int nrOfRules = dcg.getRules().size();
				int sizeOfRules = dcg.size();
				double tripleRatio = new Double(sizeOfRules)/new Double(stmtCount);
				log ="\nNr of triples="+stmtCount+" Nr of Rules="+nrOfRules+" Size of Rules="+sizeOfRules+" ratio(#triples/Rule.size())="+tripleRatio;
				
				log+= "\nLength in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB";
				long n3 = computeOrginalNTriple(model, input);
				log += "\n";
				double sizeRatio =  new Double(byteLength) / new Double(n3);
				log += "Orginal N3 length in Byte = "+n3+" = "+n3/1024+" KB ="+n3/(1024*1024)+" MB Ratio Our/BZ2="+sizeRatio;
	 			writeLogFile(input, log, true);
	
	 			
	 			log ="\n\n";
	 			log+="Nr of Subject/Objecs = "+subjectMap.size()+" Number of Properties="+propertyMap.size();
	 			writeLogFile(input, log, true);
	 			log ="\nNumber of falsePositive bloom uri checks = "+bloomErrorRate;
	 			writeLogFile(input, log, true);
	 			if(System.getProperty("user.name").equalsIgnoreCase("lyko")) 
	 				printDebug(dcg);
		 	}catch(Exception e) {
		 		File errorFile = new File(input.getAbsolutePath()+"_error.txt");
				PrintStream ps;
				try {
					ps = new PrintStream(errorFile);
					System.setErr(ps);
					e.printStackTrace();
					e.printStackTrace(ps);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				log += "\nExeption:"+e+" \n";
				writeLogFile(input, "\nExeption:"+e+" \n", true);
		 	}
		}
	
	 
//	 public long computePlainNTripleBZ2Size(Model model, File orgFile) {
//		 long size = 0;				
//		 File out = new File(orgFile.getAbsolutePath()+"_N3.n3.bz2");
//		 if(!out.exists())
//		   try {
//
//			   
//			   	ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
//				model.write(outputStream, "N3-TRIPLE");
//
//			   
//				OutputStream os = new FileOutputStream(orgFile.getAbsolutePath()+"_N3.n3.bz2");
//			    OutputStream bzos = new BZip2CompressorOutputStream(os);
//			    TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);
//
//			    byte n3s[] = outputStream.toByteArray( );
//
//			    TarArchiveEntry entry = new TarArchiveEntry(orgFile.getAbsolutePath()+"_N3.n3");
//			    entry.setSize(n3s.length);
//			    aos.putArchiveEntry(entry);
//			    aos.write(n3s);
//			    aos.closeArchiveEntry();
//			    
//			    aos.finish();
//			    aos.close();
//			    bzos.close();
//			    os.close();
//			} catch (Exception e) {
//				File errorFile = new File(orgFile.getAbsolutePath()+"_error.txt");
//				PrintStream ps;
//				try {
//					ps = new PrintStream(errorFile);
//					System.setErr(ps);
//					e.printStackTrace();
//					e.printStackTrace(ps);
//				} catch (FileNotFoundException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//				
//				log += "\nExeption:"+e+" \n";
//				writeLogFile(orgFile, "\nExeption:"+e+" \n", true);
//			}
//
//			size = out.length();
//			return size;
//	 }
	
		public long computeOrginalNTriple(Model model, File file) {
			String fileName = file.getAbsolutePath()+"_N3.n3.bz2";
			if(!file.isDirectory()) {
				fileName = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
			}
			
			File out = new File(file.getAbsolutePath()+"_N3.n3.bz2");
			if(!file.isDirectory() && file.exists() && file.canRead() && (fileName.equalsIgnoreCase("nt") || fileName.equalsIgnoreCase("n3"))) {
				try {
					InputStream fileInputStream = new BufferedInputStream (new FileInputStream (file));
					OutputStream fileOutputStream = new BufferedOutputStream (new FileOutputStream (out), 524288);
					BZip2OutputStream outputStream = new BZip2OutputStream (fileOutputStream);
		
					byte[] buffer = new byte [524288];
					int bytesRead;
					while ((bytesRead = fileInputStream.read (buffer)) != -1) {
						outputStream.write (buffer, 0, bytesRead);
					}
					outputStream.close();
					fileInputStream.close();
					} catch(Exception e) {
						e.printStackTrace();
					}
			} else {
			try {
			// InputStream fileInputStream = new BufferedInputStream (new FileInputStream (file));
			OutputStream fileOutputStream = new BufferedOutputStream (new FileOutputStream (out), 524288);
			BZip2OutputStream outputStream = new BZip2OutputStream (fileOutputStream);


			model.write(outputStream, "N3-TRIPLE");
			// model.w
			// byte[] buffer = new byte [524288];
			// int bytesRead;
			// while ((bytesRead = fileInputStream.read (buffer)) != -1) {
			// outputStream.write (buffer, 0, bytesRead);
			// }
			outputStream.close();
			} catch(Exception e) {
			e.printStackTrace();
			}
			}	
			if(out.exists())
				return out.length();
			else
				return file.length();
			}


	 
	private void writeLogFile(File source, String log, boolean append) {
		File logFile = new File(source.getAbsolutePath()+"_log"+logFileSuffix+".txt");
		try {
			
			FileWriter writer =  new FileWriter(logFile, append);
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
		int index = -1;
		switch(SPOrO) {
			case SUBJECT: 
				if(subjectMap.containsKey(uri)) {
					SubjectCount c = subjectMap.get(uri);
					index = c.nr;
					c.count++;
							break;
				}	
				else { // create new one
					SubjectCount c = new SubjectCount(subjectMap.size());
					index = c.nr;
					subjectMap.put(uri, c);
					this.indexToSubjectMap.put(index, uri);
				}
				break;
			case PREDICATE: 
				if(propertyMap.containsKey(uri)) {
					index = propertyMap.get(uri);
							break;
				}	
				else { // create new one
					index = propertyMap.size();
					propertyList.add(index, uri);

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
//		System.out.println("Resorted list...");
//		for(Entry<Integer, Integer> e: subIndexMap.entrySet()) {
//			System.out.println(e.getKey() +" => "+e.getValue());
//		}
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
				int nr = subjectIter.next();
//				nr = subIndexMap.get(nr);
				out += getUri(nr, SPO.SUBJECT);
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
			if(!rule.deleteGraph.isEmpty()) {
				out+="DELETE: ";
				Iterator<Integer> it = rule.deleteGraph.iterator();
				while(it.hasNext()) {
					Integer delSub = it.next();
					out += getUri(delSub, SPO.SUBJECT);
					if(it.hasNext())
						out+=", ";
				}
			}
			System.out.println(out);			
		}
//		for(Integer i : graph.subjectToRule.keySet()) {
//			String sOut= "s("+i+"):";
//			for(IndexRule r : graph.subjectToRule.get(i))
//				sOut+="\n\t"+r.getProfile().getProperty()+"-"+r.getProfile().getObject();
//			System.out.println(sOut);
//		}
	
	}
	
	public String getUri(Integer index, SPO SPOrO) {
		String uri = "Index="+index+"NOT_FOUND";
		switch(SPOrO) {
		case SUBJECT:
			/**@TODO check whether faster access via list is possible*/
//			if(subjectMap.containsValue(index)) {
				for(Entry<String, SubjectCount> e : subjectMap.entrySet()) {
					if(e.getValue().nr == index) {
						uri = e.getKey();
						break;
					}
				}
//			}
			break;
		case PREDICATE:
			if(propertyMap.containsValue(index)) {
				for(Entry<String, Integer> e : propertyMap.entrySet()) {
					if(e.getValue() == index) {
						uri = e.getKey();
						break;
					}
				}
			} else {
				System.out.println("Doesnt find property "+index+" in "+propertyMap);
			}
			break;
		case OBJECT:

			uri = getUri(index, SPO.SUBJECT);
			break;
		}
		return uri;
	}
	
	private void writeSingleTarFile(File input) throws IOException {

		
		 ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		    OutputStream os = new FileOutputStream(input.getAbsolutePath() + ".tar.bz2");
		    OutputStream bzos = new BZip2CompressorOutputStream(os);
		    TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);
		    //Prefixes
		    outputStream.write("\n".getBytes());
		    for (Entry<String, String>  entry : model.getNsPrefixMap().entrySet()) {
		    	outputStream.write( entry.getKey().getBytes());
		    	outputStream.write( LIST_SEP.getBytes());
		    	outputStream.write( entry.getValue().getBytes());
		    	outputStream.write( "\n".getBytes());
		    }
		    //Subject Map
		    outputStream.write((FILE_SEP+"\n").getBytes());
		    // new nr => get old
		    for(int ind = 0; ind < resortSubjectList.size(); ind++) {
//		    	outputStream.write((""+ind).getBytes());
//		    	outputStream.write( "|".getBytes());
		    	outputStream.write(indexToSubjectMap.get(resortSubjectList.get(ind).nr).getBytes());
		    	outputStream.write( "\n".getBytes());
		    }

		    outputStream.write((FILE_SEP+"\n").getBytes());
		    //Property Map
		    for(int ind = 0; ind < propertyList.size(); ind++) {
		    	outputStream.write(propertyList.get(ind).getBytes());
		    	outputStream.write("\n".getBytes());
		    }
		    
//		    for (Entry<String, Integer> property : this.propertyMap.entrySet()) {
//		    	outputStream.write( property.getKey().getBytes());
//		    	outputStream.write( "|".getBytes());
//		    	outputStream.write( property.getValue().toString().getBytes());
//		    	outputStream.write( "\n".getBytes());
//		    }
		    // Rules
		    outputStream.write((FILE_SEP+"\n").getBytes());
		    Integer prevProperty = -1;
		    
		    for(IndexRule rule : dcg.getRules()) {
			    IndexProfile profile = rule.getProfile();
			    bloomErrorRate+=profile.errorRate;
				//outputStream.write(Integer.toString(rule.getNumber()).getBytes());
				//outputStream.write(":".getBytes());
			    if(prevProperty != profile.getProperty()) {
					outputStream.write(profile.getProperty().toString().getBytes());
					outputStream.write(PROP_OBJ_SEP.getBytes());
			    }
				outputStream.write(subIndexMap.get(profile.getObject()).toString().getBytes());
				Iterator ruleIter = profile.getSubjects().iterator();
				int offset = 0;
				prevProperty = profile.getProperty();
				if(profile.size()>0) {
					outputStream.write(PO_SUBJ_SEP.getBytes());
			
					List<Integer> subjects = new LinkedList();
					for(Integer i : profile.getSubjects()) {
						subjects.add(subIndexMap.get(i));
					}
					Collections.sort(subjects);
				
					for(int i=0; i<subjects.size();i++) {
						int val = subjects.get(i);
						outputStream.write(Integer.toString(val-offset).getBytes());
						offset = val;
						if (i<subjects.size()-1){
						    outputStream.write(LIST_SEP.getBytes());
						}
					}// for each subject
				}// if rule has subjects
				if(rule.getParents().size()>0) {
					outputStream.write(SUBJ_SUPERRULE_SEP.getBytes());
					offset = 0;
					ruleIter = rule.getParents().iterator();
					while (ruleIter.hasNext()){
						IRule sr = (IRule) ruleIter.next();
					    outputStream.write(Integer.toString(sr.getNumber()-offset).getBytes());
					    offset= sr.getNumber();
					    if (ruleIter.hasNext()){
					    	outputStream.write(LIST_SEP.getBytes());
					    }
					}// for each parent
				}// end if rule has parents
				if(rule.deleteGraph.size()>0) {
					outputStream.write(DEL_SUB.getBytes());
					List<Integer> deleteSubjects = new LinkedList();
					for(Integer i : rule.deleteGraph) {
						deleteSubjects.add(subIndexMap.get(i));
					}
					Collections.sort(deleteSubjects);
					offset = 0;
					for(int i=0; i<deleteSubjects.size();i++) {
						int val = deleteSubjects.get(i);
						outputStream.write(Integer.toString(val-offset).getBytes());
						offset = val;
						if (i<deleteSubjects.size()-1){
						    outputStream.write(LIST_SEP.getBytes());
						}
					}// for each subject
				}// if rule has parents
				
				outputStream.write("\n".getBytes());
		    }// foreach rule
		    if(System.getProperty("user.name").equalsIgnoreCase("lyko")) 
		    	System.out.println(outputStream);
		    byte all[] = outputStream.toByteArray( );
		    
		    TarArchiveEntry entry = new TarArchiveEntry("all");
		    entry.setSize(all.length);
		    aos.putArchiveEntry(entry);
		    aos.write(all);
		    aos.closeArchiveEntry();

		    
		    aos.finish();
		    aos.close();
		    bzos.close();
		    os.close();
	}
	
	/**
	 * Method to reorganize subject map frequence based. That means often occuring subjects get a lesser key.
	 * @param org
	 * @return HashMap mapping the orginal key to the new one.
	 */
	public HashMap<Integer, Integer> sortFrequenceBased(HashMap<String, SubjectCount> org) {
		HashMap<Integer, Integer> map = new HashMap();
		
		resortSubjectList.addAll(org.values());
		Collections.sort(resortSubjectList);
//		System.out.println(resortSubjectList);
		for(int i = 0; i < resortSubjectList.size(); i++) {
			map.put(resortSubjectList.get(i).nr, i);
			
//			list.get(i).new_number = i;
//			System.out.println("Mapping "+list.get(i).nr+" to "+i);
		}
		return map;
	}

	@Override
	public void setLogFileSuffix(String suffix) {
		this.logFileSuffix = "_"+suffix;
	}
}
