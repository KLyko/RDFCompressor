package de.uni_leipzig.simba.compress;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.enums.RDFNotation;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.data.IRule;
import de.uni_leipzig.simba.data.IndexCompressedGraph;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.data.InstantCompressedGraph;
import de.uni_leipzig.simba.io.ModelLoader;
import de.uni_leipzig.simba.util.PrefixHelper;

/**
 * Class to create compressed graph (rules, their super rules) while iterating the model.
 * This is done by:
 * 	1. querying the RDF model for all properties
 * 	2. get all objects which are RDF resources
 * 	3. for each of this p-o pair get all subjects
 * Thus, we can construct the complete rules at once:
 * Don't have to check if a rule already exists. And also all rules are already sorted by property. 
 * No empty, or redundant rules exist. Thereby, we can significantly limit the complexity.
 * 
 * @author Klaus Lyko
 *
 */
public class ModelCompressor extends BasicCompressor implements Compressor, Runnable {
	boolean hdt = true;
	boolean abbreviate = false;
	public final static String valueModelHDTName = "valueModel.hdt";
	private Model readModel()  {
		Model model = ModelFactory.createDefaultModel();
 		try {
			model = ModelLoader.getModel(input.getAbsolutePath());
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
 		return model;
	}
	
	public ModelCompressor(File input) {
		setFile(input);
	}
	
	@Override
	public void run() {
		compress();
	}

	@Override
	public void compress() {
		
		log = "--> Combined creating rules and computing super rules\n";
		log += input.getAbsolutePath()+"\n";
		long byteLength = input.length();
		log+= "Length in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB\n\n";

 		status.update("Initialized", "Loading Jena Model");
// 		feedback.timePassed
		setChanged();
 		notifyObservers(status);
		writeLogFile(input, log, false);
	/*############## Reading Model, creating all rules ################################################*/
		long start = System.currentTimeMillis();
		Model model = readModel();
		long middle = System.currentTimeMillis();
		
 		String print = "Loaded model took: " + (middle-start) + " milli seconds = "+ (middle-start) /1000 +" seconds";
		System.out.println(print);
		writeLogFile(input, print, true);
		setChanged();
		status.update("Loaded Model in "+(middle-start) + " milli seconds = "+ (middle-start) /1000 +" seconds", "Reading and building rules.");
 		notifyObservers(status);
 		compress(model, "");
	}
	
	public void compress(Model model, String logExt) {
		this.model = model;
		if(logExt.length() > 0) {
			logFileSuffix = logExt;
			log = "Compressing Model direct: "+logExt+"\n";
			writeLogFile(input, log, false);
		}
		long start = System.currentTimeMillis();
		long middle = System.currentTimeMillis();
		valueModel = ModelFactory.createDefaultModel();
		InstantCompressedGraph ruleGraph = new InstantCompressedGraph(model.size(), true, delete);
		shortToUri.putAll(model.getNsPrefixMap());
		
		/*Query all properties*/
 		String query = "SELECT DISTINCT ?p "+
 					"WHERE {" +
 					"?s ?p ?o" +
 					"}";
 		QueryExecution qe = QueryExecutionFactory.create(query, model);
 		ResultSet rs = qe.execSelect();
 		// some statistics
 		int stmtCount = 0; int stmtCountByProperty=0;//count statements
 		int objByProp=0; int valueObjectByProp=0;
 		int valueStmtCount = 0;//count statements where oi isn't a resource
 		int propertyCount = 0;
		long lastPropTime = System.currentTimeMillis();
		double durationRuleAdding = 0; double valueModelCreation = 0;
		double sumRuleCreation = 0; double sumValueModelCreation =0;
		
 		while(rs.hasNext()) {
 			//statistics
 			double timeSpend = System.currentTimeMillis()-lastPropTime;
 			String stat = "Handled "+(++propertyCount)+" Property. Last one took "+timeSpend+" millis = "+(timeSpend/1000)+"s = "+(timeSpend/60000)+" min\n";
 				   stat+="Last: objByProp="+objByProp+", valueObjectByProp="+valueObjectByProp+" stmtByProp="+stmtCountByProperty+"\n";
 				   stat+="\t stmtByObjectPropPair="+stmtCountByProperty+", valueStmt="+valueStmtCount+", stmt="+stmtCount+"\n";
 				   if(timeSpend>0)
 					   stat+="% Rule add("+durationRuleAdding+")= "+((durationRuleAdding/timeSpend)*100)+" % ValueModel("+valueModelCreation+")= "+((valueModelCreation/timeSpend)*100)+"\n";
 			System.out.println(stat);
			lastPropTime = System.currentTimeMillis();
			stmtCountByProperty=0; objByProp=0; valueObjectByProp=0;
			sumRuleCreation += durationRuleAdding;
			sumValueModelCreation += valueModelCreation;
			durationRuleAdding = 0; valueModelCreation = 0;
			
 			QuerySolution qs = rs.nextSolution();
			Resource res_p = qs.getResource("?p");
			Property p = model.getProperty(res_p.getURI());
//			System.out.println("Parsing Resource "+res_p+" to Property "+p);
			String uri = "";
			if(p.isURIResource()) {
				uri= p.getURI();
				
//				if(model.shortForm(uri).length() == uri.length()) {
//					String[] pref = PrefixHelper.generatePrefix(uri);
//					System.out.println("Created prefix for "+uri+":\n"+pref[0]+" -> "+pref[1]);
//					model.setNsPrefix(pref[0], pref[1]);
//				}
				uri = model.shortForm(uri);
			}else {
				uri = "_:"+p.getId().toString();
				System.out.println("Created anon property id "+uri+" anon: "+p.getId().getLabelString());
			}
			int indexP = this.addIndex(uri, SPO.PREDICATE);
			NodeIterator nodeIter = model.listObjectsOfProperty(p);
			while(nodeIter.hasNext()) { //nest
				RDFNode o_node=nodeIter.next();
				if(o_node.isResource()) {
					// we have an p - o pair
					objByProp++;
					Resource o_res = o_node.asResource();
					if(o_res.isURIResource()) {
						uri = o_node.asResource().getURI();
						if(abbreviate && model.shortForm(uri).length() == uri.length()) {
							try {
								String[] pref = PrefixHelper.generatePrefix(uri);
//								System.out.println("Created prefix for "+uri+":"+pref[0]+" -> "+pref[1]);
								model.setNsPrefix(pref[0], pref[1] );
							}catch(Exception e) {
								
							}							
						}
						uri = model.shortForm(uri);
					}// o_node is resource and has URI
					else {
						uri = "_:"+o_res.getId().toString();
						System.out.println("Created obj AnonID: "+uri+" anon.label: "+o_res.getId().getLabelString());						
					}
					
					int indexO = addIndex(uri, SPO.OBJECT); 
					IndexProfile profile = new IndexProfile(indexP, indexO);
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						stmtCount++; stmtCountByProperty++;
						Resource s = subIter.next();
						if(s.isURIResource()) {
							uri = s.getURI();
							if(abbreviate && model.shortForm(uri).length() == uri.length()) {
								try {
									String[] pref = PrefixHelper.generatePrefix(uri);
//									System.out.println("Created prefix for "+uri+":"+pref[0]+" -> "+pref[1]);
									model.setNsPrefix(pref[0], pref[1] );
								}catch(Exception e) {}	
							}
							uri = model.shortForm(uri);
						} else {//is blank node
							uri = "_:"+s.getId().toString();
							System.out.println("Created AnonID for s: "+uri+" anon.label: "+s.getId().getLabelString());
						}
						profile.addSubject(addIndex(uri, SPO.SUBJECT));
					}					
					//creating and adding rule
					long startRuleAdd = System.currentTimeMillis();
					IndexRule rule = new IndexRule(profile);
					try {
						ruleGraph.addRule(rule, profile.getSubjects());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					durationRuleAdding += (System.currentTimeMillis()-startRuleAdd);
//					System.out.println("Added rule "+rule);					
				} else { // o isn't a resource
					valueObjectByProp++;
					ResIterator subIter = model.listResourcesWithProperty(p, o_node);
					while(subIter.hasNext()) {
						long startValueAdd = System.currentTimeMillis();
						Resource r = subIter.next();
						if(r.isURIResource()) {
							uri = r.getURI();
							if(abbreviate && model.shortForm(uri).length() == uri.length()) {
								try {
									String[] pref = PrefixHelper.generatePrefix(uri);
//									System.out.println("Created prefix for "+uri+":"+pref[0]+" -> "+pref[1]);
									model.setNsPrefix(pref[0], pref[1] );
								}catch(Exception e) {}	
							}
							uri = model.shortForm(uri);
						} else { // is blank node
							uri = "_:"+r.getId().toString();
							System.out.println("Created AnonID for s_val: "+uri+" anon.label: "+r.getId().getLabelString());
						}
//						if(!hdt) {
							Resource sIR = valueModel.createResource(""+addIndex(uri, SPO.SUBJECT));
							Property pIR = valueModel.createProperty(""+indexP);
							valueModel.add(sIR, pIR, o_node);
//						} else {
//							Resource sIR = valueModel.createResource(uri);
//							Property pIR = valueModel.createProperty(p);
						
//							valueModel.add(r, p, o_node);
//						}
						
						valueModelCreation += (System.currentTimeMillis()-startValueAdd);
						valueStmtCount++; stmtCount++;
//						System.out.println("new Value statement <"+sIR+"><"+pIR+">"+o_node);		
					}
					
				}
			}
//			out = out.substring(0, out.length()-2);
//			out += "}";
//			System.out.println(out);
		}//for all properties
 		long timeRules = (System.currentTimeMillis()-middle);
 		String print = "Created all rules" + timeRules + " milli seconds = " + timeRules/1000 +" seconds\n";
// 		print += "% Rule Adding = "+((sumRuleCreation/timeRules)*100)+", %ValueModelCreation="+((sumValueModelCreation/timeRules)*100);
		System.out.println(print);
		writeLogFile(input, print, true);
 		middle=System.currentTimeMillis();
 		/*############## compute super rules once ###########################################*/
//		middle = System.currentTimeMillis();
 		status.update("Created all rules" + timeRules + " milli seconds = " + timeRules/1000 +" seconds.", "Computing super rules.");
		setChanged();
 		notifyObservers(status);
// 		this.notifyAll();
// 		this.wait();
 		ruleGraph.computeAllSuperRulesOnce();
		print = "Computing super rules: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
		System.out.println(print);
		writeLogFile(input, print, true);
 		middle = System.currentTimeMillis();
 		status.update("Super rules computed", "Removing redundancies");
 		/*############## remove redundancies ################################################*/
 		ruleGraph.removeRedundantParentRules();
 		print = "Removed redundancies in: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
		System.out.println(print);
		writeLogFile(input, print, true);			
		status.update("Removed redundancies in: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds", "Sorting subjects by frequence.");
		setChanged();
 		notifyObservers(status);
 		middle =System.currentTimeMillis();
 		/*############## reorganize: sort subjects per frequence ############################*/
 		subIndexMap = sortSubjectsFrequenceBased(subjectMap);
 		long end = System.currentTimeMillis();
		print = "Sorting Frequence Based took "+(end-middle)+" ms = "+((end-middle)/1000)+ " s";
		System.out.println(print);
		status.update("Sorting Frequence Based took "+(end-middle)+" ms = "+((end-middle)/1000)+ " s", "Writing tar file.");
		writeLogFile(input, print, true);
		setChanged();
 		notifyObservers(status);
 		middle = System.currentTimeMillis();
 		/*############## writing tar file ############################*/
 		File outFile=null;
 		try{
 			outFile=writeBZip2TarFile(input, ruleGraph);			   
		}
		catch (IOException ioe){
			ioe.printStackTrace();
			log += "\nExeption:"+ioe+" \n";
			writeLogFile(input, "\nExeption:"+ioe+" \n", true);
		}
 		
 		print += "\nWriting files took: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds";
		System.out.println(print);
		writeLogFile(input, print, true);
		status.update("Written files. Writing files took: " + (System.currentTimeMillis()-middle) + " milli seconds = " + (System.currentTimeMillis()-middle)/1000 +" seconds.", "Computing addtional stuff...");
		setChanged();
 		notifyObservers(status);
		
 		print = "Overall : " + (System.currentTimeMillis()-start) + " milli seconds = " + (System.currentTimeMillis()-start)/1000 +" seconds";
		System.out.println(print);
		writeLogFile(input, print, true);
		
		long byteLength = outFile.length();
		
		int nrOfRules = ruleGraph.getRules().size();
		int sizeOfRules = ruleGraph.size();
		
		
		double tripleRatio = new Double(sizeOfRules)/new Double(stmtCount);
		log ="\nNr of triples="+stmtCount+"(of which have literal objects="+valueStmtCount+") Nr of Rules="+nrOfRules+" Size of Rules="+sizeOfRules+" ratio(#triples/Rule.size())="+tripleRatio;
		
		log+= "\nLength in Bytes = "+ byteLength + "= "+byteLength/1024 +" KB = "+ byteLength/(1024*1024)+" MB";
		middle = System.currentTimeMillis();
		long n3 = computeOrginalNTriple(model, input);
		long durBZip2 = System.currentTimeMillis()-middle;
		log += "\n";
		double sizeRatio =  new Double(byteLength) / new Double(n3);
		log += "Orginal N3 length in Byte = "+n3+" = "+n3/1024+" KB ="+n3/(1024*1024)+" MB Ratio Our/BZ2="+sizeRatio;
			writeLogFile(input, log, true);			
			log ="\n\n";
			log+="Compressing NT with plainBzip2 required "+durBZip2+" milli seconds = "+durBZip2/1000+" seconds\n";
			log+="Nr of Subject/Objecs = "+subjectMap.size()+" Number of Properties="+propertyMap.size();
			log+="\nNr of single Subjects/Objects " + nrOfSingleSubs;
			log+="\nNr of atomic rules="+nrOfAtomicRules+"; Nr of parents="+nrOfParents+"; Nr of Delete Rules "+nrOfDeleteRules+"; Size of delete entries="+sizeOfDeleteEntries;
		writeLogFile(input, log, true);
			log ="\nNumber of falsePositive bloom uri checks = "+bloomErrorRate;

			log+="\n Durations RuleCreation="+sumRuleCreation+", sumValueModelCreation="+sumValueModelCreation;
			
		writeLogFile(input, log, true);
		if(System.getProperty("user.name").equalsIgnoreCase("lyko")) 
			printDebug(ruleGraph, System.out, 20);
		
		status.setFinished();
		status.update("Finished computation in "+(System.currentTimeMillis()-start) + " milli seconds = " + (System.currentTimeMillis()-start)/1000 +" seconds", "");
		setChanged();
 		notifyObservers(status);
			
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

	@Override
	public void setLogFileSuffix(String suffix) {
		this.logFileSuffix = "_"+suffix;
	}
	
	
	public static void main(String args[]) {
		File file = new File("resources/n3.n3");
		ModelCompressor compr = new ModelCompressor(file);
		compr.setLogFileSuffix("combined");
		compr.compress();
	}
	
	private OutputStream writeRulesToOutputStream(OutputStream outputStream, IndexCompressedGraph ruleGraph) throws IOException {
	    //Prefixes
//	    outputStream.write("\n".getBytes());

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
//	    int nrOfObjects = resortObjectList.size();
//	    for(int ind = 0; ind < Math.max(nrOfSubjects, nrOfObjects); ind++) {
	    for(int ind = 0; ind < nrOfSubjects; ind++) {
//	    	outputStream.write((""+ind).getBytes());
//	    	outputStream.write( "|".getBytes());
	    	if(ind < nrOfSubjects)
	    		outputStream.write(indexToSubjectMap.get(resortSubjectList.get(ind).nr).getBytes());
//	    	if(ind < nrOfObjects)
//	    		outputStream.write("\2".getBytes());
//	    	if(ind < nrOfObjects)
//	    		outputStream.write(indexToObjectMap.get(resortObjectList.get(ind).nr).getBytes());	
	    	outputStream.write( "\n".getBytes());
	    }

	    outputStream.write((FILE_SEP+"\n").getBytes());
	    //Property Map
	    for(int ind = 0; ind < propertyList.size(); ind++) {
	    	outputStream.write(propertyList.get(ind).getBytes());
	    	outputStream.write("\n".getBytes());
	    }
	    
//	    for (Entry<String, Integer> property : this.propertyMap.entrySet()) {
//	    	outputStream.write( property.getKey().getBytes());
//	    	outputStream.write( "|".getBytes());
//	    	outputStream.write( property.getValue().toString().getBytes());
//	    	outputStream.write( "\n".getBytes());
//	    }
	    // Rules
	    outputStream.write((FILE_SEP+"\n").getBytes());
	    Integer prevProperty = -1;
	    
	    for(IndexRule rule : ruleGraph.getRules()) {
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
//			outputStream.write(objIndexMap.get(profile.getObject()).toString().getBytes());
		    outputStream.write(subIndexMap.get(profile.getObject()).toString().getBytes());
			Iterator ruleIter = profile.getSubjects().iterator();
			int offset = 0;
			prevProperty = profile.getProperty();
			if(profile.size()>0) {
				outputStream.write(PO_SUBJ_SEP.getBytes());
		
				List<Integer> subjects = new LinkedList<Integer>();
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
				List<Integer> deleteSubjects = new LinkedList<Integer>();
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
	    return outputStream;
	}

	/**
	 * Writes single Tar Entries 
	 * @param input
	 * @param ruleGraph
	 * @return
	 * @throws IOException
	 */
	private File writeBZip2TarFile(File input, IndexCompressedGraph ruleGraph) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		File file = new File(input.getAbsolutePath() +logFileSuffix+ ".cp.tar.bz2");
		OutputStream os = new FileOutputStream(file, false);
		OutputStream bzos = new BZip2CompressorOutputStream(os);
		TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);
		//Write rules to output stream
		writeRulesToOutputStream(outputStream, ruleGraph);
		byte[] allRules = outputStream.toByteArray();
		TarArchiveEntry ruleEntry = new TarArchiveEntry(ruleFileName);
		ruleEntry.setSize(allRules.length);
		aos.putArchiveEntry(ruleEntry);
		aos.write(allRules);
		aos.closeArchiveEntry();
		//---------------------------- --------------------------- -------------------
		if(!hdt) {
			
			outputStream = new ByteArrayOutputStream( );
			Model finalValueModel = createFinalValueModel();
			if(finalValueModel.size()>0) {
				finalValueModel.write(outputStream, "TURTLE");
				byte[] turtle = outputStream.toByteArray();
				writeLogFile(input, "Serializing value model of size "+finalValueModel.size()+" using Turtle", true);
				TarArchiveEntry turtleEntry = new TarArchiveEntry(valueModelFileName);
				turtleEntry.setSize(turtle.length);
				aos.putArchiveEntry(turtleEntry);
				aos.write(turtle);
				aos.closeArchiveEntry();
			}
		} else {
			
		
		//------------------------hdt--------------------------------------------------
		try{
			String tmpDir = System.getProperty("java.io.tmpdir");
			FileOutputStream fos = new FileOutputStream(tmpDir + "/"+getFileName(input)+"data.nt");
			
			outputStream = new ByteArrayOutputStream( );
			Model finalValueModel = createFinalValueModel();
			if(finalValueModel.size()>0) {
				finalValueModel.write(fos, "N-TRIPLE");
				fos.close();
				writeLogFile(input, "Serializing value model of size "+finalValueModel.size()+" using HDT", true);
				HDT hdt = HDTManager.generateHDT(
					tmpDir + "/"+getFileName(input)+"data.nt",
					"urn:rdfcomp",
					RDFNotation.parse("ntriples"),
					new HDTSpecification(),
					null
				);
				hdt.saveToHDT(outputStream, null);
				
				byte[] turtle = outputStream.toByteArray();
				TarArchiveEntry turtleEntry = new TarArchiveEntry(valueModelHDTName);
				turtleEntry.setSize(turtle.length);
				aos.putArchiveEntry(turtleEntry);
				aos.write(turtle);
				aos.closeArchiveEntry();
			} 
		}catch (Exception e){e.printStackTrace();};
			
			
		}	
		aos.finish();
		aos.close();
		bzos.close();
		os.close();
		return file;
		}
//	/**
//	 * Old method concatenates all in one bzip2 file
//	 * @param input2
//	 * @param ruleGraph
//	 * @throws IOException
//	 */
//	@Deprecated
//	private File writeSingleBzip2File(File input2, IndexCompressedGraph ruleGraph) throws IOException{
//		File file = new File(input.getAbsolutePath() +logFileSuffix+".cp.bz2");
//		OutputStream fos = new BufferedOutputStream(new FileOutputStream(file, true));
//        BZip2CompressorOutputStream  outputStream = new BZip2CompressorOutputStream (fos);
//        writeRulesToOutputStream(outputStream, ruleGraph);
//  
//		Model finalValueModel = createFinalValueModel();
//	    if(finalValueModel.size()>0) {
//	  		outputStream.write((FILE_SEP+"\n").getBytes());
//	  		finalValueModel.write(outputStream, "TURTLE");
//		}
//		outputStream.close();
//        fos.close();
//        return file;
//	}
	
	private String getFileName(File f) {
		String fn = f.getName();
		if(fn.endsWith("/"))
			fn = fn.substring(0,fn.length()-1);
		if(fn.endsWith("\\"))
			fn = fn.substring(0,fn.length()-1);
		return fn;
	}
	
	public void setHDT(boolean activate) {
		this.hdt = activate;
	}
}
