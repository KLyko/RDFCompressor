package de.uni_leipzig.simba.compress;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.data.IRule;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.io.ModelLoader;
/**
 * Implementation of an index based compression:
 * Creates Indexes for all subjects, objects, predicates. And operates on them
 * @author Klaus Lyko
 *
 */
public class IndexBasedCompressor extends BasicCompressor implements Compressor, Runnable
{
	
	 public IndexBasedCompressor(File path, int i) {super(path, i);}
	 public IndexBasedCompressor() {super();}

	public void compress() {
		 	log += input.getAbsolutePath()+"\n";
		
			long byteLength = input.length();
			log+= "Length in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB\n\n";
			writeLogFile(input, log, false);
			
		 	long start = System.currentTimeMillis();
		 	try {
		 		setChanged();
		 		notifyObservers("Loading Model...");
//		 		this.notifyAll();
//				this.wait();
		 		valueModel = ModelFactory.createDefaultModel();
		 
		 		model = ModelLoader.getModel(input.getAbsolutePath());
		 	
				shortToUri.putAll(model.getNsPrefixMap());
				setChanged();
		 		notifyObservers("Creating Rules...");
//		 		this.notifyAll();
//		 		this.wait();
				// build inverse list of p/o tuples
				dcg = new IndexCompressedGraph(model.size(), true, delete);
				
				StmtIterator iter = model.listStatements();
				
				long middle = System.currentTimeMillis();
				long middle2 = System.currentTimeMillis();
				String print = "Loading model took: " + (middle-start) + " milli seconds = "+ (middle-start) /1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
				int valueStmtCount = 0;
				int stmtCount = 0;
				while( iter.hasNext() ){
					Statement stmt = iter.next();
					if(stmt.getObject().isResource()) {
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
					} else { // object is no Resource (e.g. literal)
						String s = stmt.getSubject().toString();
						try{
							s = model.shortForm(s);
						} catch(NullPointerException npe){ /*bnode*/ }
						
						String p = stmt.getPredicate().getURI();
						try{
							p = model.shortForm(p);
						} catch(NullPointerException npe){ /*bnode*/ }
						// alse use indices
						int indexS = addIndex(s, SPO.SUBJECT);
						int indexP = addIndex(p, SPO.PREDICATE);
						Resource sIR = valueModel.createResource(""+indexS);
						Property pIR = valueModel.createProperty(""+indexP);
						valueModel.add(sIR, pIR, stmt.getObject());
						valueStmtCount++;
					}
					
				}
				
				print = "Reading all rules: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
				
				print = "#Rule statements="+stmtCount+" , #Value statement="+valueStmtCount;
				System.out.println(print);
				writeLogFile(input, print, true);
				
				middle = System.currentTimeMillis();
				setChanged();
		 		notifyObservers("Computing SuperRules...");
//		 		this.notifyAll();
//		 		this.wait();
				dcg.computeSuperRules();
				print = "Computing super rules: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);
	//			log += print +"\n";
	
				middle = System.currentTimeMillis();
				setChanged();
		 		notifyObservers("Removing redundant Rules...");
//		 		this.notifyAll();
//		 		this.wait();
				dcg.removeRedundantParentRules();
				
				print = "Removing redundancies: : " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
				System.out.println(print);
				writeLogFile(input, print, true);				
				System.out.println(dcg.log);
				writeLogFile(input, dcg.log, true);
				
	//			log += print +"\n";
				setChanged();
		 		notifyObservers("Sorting Subjects...");
//		 		this.wait();
				middle = System.currentTimeMillis();
				subIndexMap = sortSubjectsFrequenceBased(subjectMap);
//				objIndexMap = sortObjectsFrequenceBased(objectMap);
				long end = System.currentTimeMillis();
				print = "Sorting subjects Frequence Based took "+(end-middle)+" ms = "+((end-middle)/1000)+ " s";
				System.out.println(print);
				writeLogFile(input, print, true);
				setChanged();
		 		notifyObservers(print+" start writing tar file...");
//				middle = System.currentTimeMillis();
////				dcg.getResortedRules();
//				end = System.currentTimeMillis();
//				print = "Sorting Frequence Based took "+(end-middle)+" ms = "+((end-middle)/1000)+ " s";
//				System.out.println(print);
//				writeLogFile(input, print, true);
				
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
				File outFile = new File(input.getAbsolutePath() + ".cp.bz2");
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
	 			log+="\nNr of single Subjects/Objects " + nrOfSingleSubs;
	 			log+="\nNr of atomic rules="+nrOfAtomicRules+"; Nr of parents="+nrOfParents+"; Nr of Delete Rules "+nrOfDeleteRules+"; Size of delete entries="+sizeOfDeleteEntries;
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
					e1.printStackTrace();
				}
				
				log += "\nExeption:"+e+" \n";
				writeLogFile(input, "\nExeption:"+e+" \n", true);
		 	}
		 	setChanged();
		 	notifyObservers("finished");
		}

	private Model createFinalValueModel() {
		Model finalModel = ModelFactory.createDefaultModel();
		
		StmtIterator iter = valueModel.listStatements();
		while( iter.hasNext() ) {
			Statement stmt = iter.next();
			int sI = Integer.parseInt(stmt.getSubject().getURI());
			Resource sR = finalModel.createResource(""+subIndexMap.get(sI));
			finalModel.add(sR, stmt.getPredicate(), stmt.getObject());
		}
		return finalModel;
	}
	
	private void writeSingleTarFile(File input) throws IOException {
		setChanged();
 		notifyObservers("Compress with BZip2...");
// 		this.notifyAll();
// 		try {
//			this.wait();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		 OutputStream fos = new BufferedOutputStream(new FileOutputStream(input.getAbsolutePath() + ".cp.bz2"));
         BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);
		    //Prefixes
//		    outputStream.write("\n".getBytes());
		    for (Entry<String, String>  entry : model.getNsPrefixMap().entrySet()) {
		    	outputStream.write( entry.getKey().getBytes());
		    	outputStream.write( LIST_SEP.getBytes());
		    	outputStream.write( entry.getValue().getBytes());
		    	outputStream.write( "\n".getBytes());
		    }
		    //Subject Map
		    outputStream.write((FILE_SEP+"\n").getBytes());
		    // new nr => get old
		    int nrOfSubjects = resortSubjectList.size();
//		    int nrOfObjects = resortObjectList.size();
//		    for(int ind = 0; ind < Math.max(nrOfSubjects, nrOfObjects); ind++) {
		    for(int ind = 0; ind < nrOfSubjects; ind++) {
//		    	outputStream.write((""+ind).getBytes());
//		    	outputStream.write( "|".getBytes());
		    	if(ind < nrOfSubjects)
		    		outputStream.write(indexToSubjectMap.get(resortSubjectList.get(ind).nr).getBytes());
//		    	if(ind < nrOfObjects)
//		    		outputStream.write("\2".getBytes());
//		    	if(ind < nrOfObjects)
//		    		outputStream.write(indexToObjectMap.get(resortObjectList.get(ind).nr).getBytes());	
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
		    	if(rule.isAtomic())
		    		nrOfAtomicRules++;
			    IndexProfile profile = rule.getProfile();
			    bloomErrorRate+=profile.errorRate;
				//outputStream.write(Integer.toString(rule.getNumber()).getBytes());
				//outputStream.write(":".getBytes());
			    if(prevProperty != profile.getProperty()) {
					outputStream.write(profile.getProperty().toString().getBytes());
					outputStream.write(PROP_OBJ_SEP.getBytes());
			    }
//				outputStream.write(objIndexMap.get(profile.getObject()).toString().getBytes());
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
					nrOfParents++;
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
					nrOfDeleteRules++;
					outputStream.write(DEL_SUB.getBytes());
					List<Integer> deleteSubjects = new LinkedList();
					sizeOfDeleteEntries += rule.deleteGraph.size();
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
		    // value model
		    outputStream.write((FILE_SEP+"\n").getBytes());
		    Model finalValueModel = createFinalValueModel();
		    finalValueModel.write(outputStream, "TURTLE");
		    outputStream.close();
            fos.close();
	}
	
	

	@Override
	public void run() {
		compress();
	}
	

	@Override
	public void setLogFileSuffix(String suffix) {
		this.logFileSuffix = "_"+suffix;
	}
	
}
