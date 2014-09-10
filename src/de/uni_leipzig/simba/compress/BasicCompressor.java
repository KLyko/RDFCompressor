package de.uni_leipzig.simba.compress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import com.hp.hpl.jena.rdf.model.Model;

import de.uni_leipzig.simba.data.IRule;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.data.SubjectCount;

public class BasicCompressor extends Observable implements IndexBasedCompressorInterface {
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
	
	int nrOfSingleSubs = 0;
	int nrOfAtomicRules = 0;
	int nrOfDeleteRules = 0;
	int sizeOfDeleteEntries = 0;
	int nrOfParents = 0;
	
	String log = "-->One HashMap for object and subject\n" +
			"--> sorted by props\n" +
			"--> one tar archive\n " +
			"--> sort uris frequence based\n" +
			"--> additional value model: use normal N3 serialization";

	HashMap<String, String> shortToUri = new HashMap();
	HashMap<String, SubjectCount> subjectMap = new HashMap();
//	HashMap<String, SubjectCount> objectMap = new HashMap();
	HashMap<Integer, String> indexToSubjectMap = new HashMap();
//	HashMap<Integer, String> indexToObjectMap = new HashMap();
	List<SubjectCount> resortSubjectList = new ArrayList<SubjectCount>();
//	List<SubjectCount> resortObjectList = new ArrayList<SubjectCount>();
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
//	HashMap<Integer, Integer> objIndexMap;
	
	IndexCompressedGraph dcg; Model model;
	Model valueModel;
	int delete;
	File input;
	
	

	public BasicCompressor(File input, int delete) {
		this.input = input;
		this.delete = delete;
	}
	
	public void setFile(File input){
		this.input = input;
	}
	public void setDelete(int delete) {
		this.delete = delete;
	}
	
	public BasicCompressor() {
		this(null, 0);
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
			/**@TODO check whether faster access via list is possible*/
//			if(subjectMap.containsValue(index)) {
//				for(Entry<String, SubjectCount> e : objectMap.entrySet()) {
//					if(e.getValue().nr == index) {
//						uri = e.getKey();
//						break;
//					}
//				}
			uri = getUri(index, SPO.SUBJECT);
//			}
			break;
		}
		return uri;
	}
	

	protected long computeOrginalNTriple(Model model, File file) {
		String fileName = file.getAbsolutePath()+"_N3.n3.bz2";
		if(!file.isDirectory()) {
			fileName = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
		}
		File out = new File(file.getAbsolutePath()+"_N3.n3.bz2");
		if(!file.isDirectory() && file.exists() && file.canRead() && (fileName.equalsIgnoreCase("nt") || fileName.equalsIgnoreCase("n3"))) {
			try {
				InputStream fileInputStream = new BufferedInputStream (new FileInputStream (file));
				OutputStream fos = new BufferedOutputStream(new FileOutputStream(out));
		        BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);
		        
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
				OutputStream fos = new BufferedOutputStream(new FileOutputStream(out));
		        BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);
			    
				model.write(outputStream, "N-TRIPLE");
			
				outputStream.close();
				fos.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(out.exists())
			return out.length();
		else
			return file.length();
}
	
	/**
	 * Method to reorganize subject map frequence based. That means often occuring subjects get a lesser key.
	 * @param org
	 * @return HashMap mapping the orginal key to the new one.
	 */
	public HashMap<Integer, Integer> sortSubjectsFrequenceBased(HashMap<String, SubjectCount> org) {
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
	
//	public HashMap<Integer, Integer> sortObjectsFrequenceBased(HashMap<String, SubjectCount> org) {
//		HashMap<Integer, Integer> map = new HashMap();
//		
//		resortObjectList.addAll(org.values());
//		Collections.sort(resortObjectList);
////		System.out.println(resortSubjectList);
//		for(int i = 0; i < resortObjectList.size(); i++) {
//			map.put(resortObjectList.get(i).nr, i);
//			
////			list.get(i).new_number = i;
////			System.out.println("Mapping "+list.get(i).nr+" to "+i);
//		}
//		return map;
//	}
	
	protected void printDebug(IndexCompressedGraph graph) {
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
//			String out= "("+rule.atomNr+" "+rule.isAtomic()+")"+rule.getNumber()+": ";
			String out= rule.getNumber()+": ";
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
				out += "\t {";
				Iterator<IRule<IndexProfile>> it = rule.getParents().iterator();
				while(it.hasNext()) {
					out+= it.next().getNumber();
					if(it.hasNext())
						out += ", ";
				}
				out += "}";
				
			}
			if(!rule.deleteGraph.isEmpty()) {
				out+="\tDELETE: ";
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
	

	 
	protected void writeLogFile(File source, String log, boolean append) {
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
					if(c.count==2)
						nrOfSingleSubs--;
							break;
				}	
				else { // create new one
					nrOfSingleSubs++;
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
				if(subjectMap.containsKey(uri)) {
					SubjectCount c = subjectMap.get(uri);
					c.isSubject = false;
					index = c.nr;
					c.count++;
					if(c.count==2)
						nrOfSingleSubs--;
							break;
				}	
				else { // create new one
					nrOfSingleSubs++;
					SubjectCount c = new SubjectCount(subjectMap.size(), false);
					index = c.nr;
					subjectMap.put(uri, c);
					this.indexToSubjectMap.put(index, uri);
				}
				break;
		}
		return index;
	}	
	
/*##########################Observer constants##############################################*/
	
}
