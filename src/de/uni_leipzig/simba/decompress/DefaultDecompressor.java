package de.uni_leipzig.simba.decompress;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.compress.BasicCompressor;
import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.compress.ModelCompressor;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.io.ModelLoader;
import de.uni_leipzig.simba.io.ObserverFeedback;
import de.uni_leipzig.simba.io.Status;
/**
 * Standard implementation of the decompressor for serialized SCARO files.
 * @author Klaus Lyko
 *
 */
public class DefaultDecompressor extends Observable implements DeCompressor, Runnable{
	
	boolean hasDeleteRules = false;
	
	HashMap<Integer, String> subjects = new HashMap<Integer, String>();
	HashMap<Integer, String> properties = new HashMap<Integer, String>();
	HashMap<String, String> abbrev = new HashMap<String, String>();
	
	HashMap<Integer, IndexRule> ruleMap = new HashMap<Integer, IndexRule>();
	
	File file;
	public Status status;
	public ObserverFeedback feedback;
	
	Model globalModel = ModelFactory.createDefaultModel();
	
	public DefaultDecompressor(File input) {
		this.file = input;
		feedback = new ObserverFeedback();
 		feedback.currentStatus = status;
	}
	
	@Override
	public void run() {
		try{
			decompress(this.file);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public Model decompress(File file) throws IOException, CompressorException, ArchiveException {
		status = new Status("Begin decompression", 0, 3);
		Map<String, File> fileMap = unTar(file, new File(file.getParent()));
		BufferedReader br = getBufferedReaderForRuleFile(fileMap.get(BasicCompressor.ruleFileName));
		
		setChanged();
		status.update("Initialized", "Parsing file");
 		notifyObservers(status);
		
		String line;
		int stage = 0;
		int ruleNr = 0;
		int lastProp = -1;
		 while ((line = br.readLine()) != null) {
			 if(line.equalsIgnoreCase(IndexBasedCompressor.FILE_SEP)) {
				 stage++;
				 continue;
			 }
			 if(line.length()>0) {
				 switch(stage) {
				 	case 0: parseAbbreviations(line);break;
				 	case 1: parseSubjects(line);break;
				 	case 2: parseProperties(line);break;
				 	case 3: lastProp = parseRule(line, lastProp, ruleNr); ruleNr++;break;
				 }
			 }
		 }
		 // structure loaded
//		 for(Entry<Integer, IndexRule> e: ruleMap.entrySet()) {
//			System.out.println(e.getKey()+": "+e.getValue()+" "+e.getValue().getParentIndices());
//		 }
//		 
		 ArrayList<String> triples = new ArrayList<String>(22);
		 
		setChanged();
		status.update("File successfully parsed", "Building triples");
	 	notifyObservers(status);
		if(!hasDeleteRules) {
//			int count = 0;

		 for(Integer rNr : ruleMap.keySet()) {
//			 count++;
			 Set<String> nts = buildNTriples(rNr, new HashSet<Integer>(), new HashSet<Integer>());
			 triples.addAll(nts);
//			 System.out.println("Building triples of "+count+" of "+ruleMap.size()+" Rules: Nr of triples: "+triples.size());
		 }
		} else {
			 System.out.println("Model has delete rules");
//			 for(Entry<Integer, IndexRule> entry : ruleMap.entrySet()) {
//				 System.out.println(entry.getValue() + "Par:\t" +entry.getValue().getParentIndices()+ "\tdel: "+entry.getValue().deleteGraph);
//			 }
//			 addSubjectsToSuperRules();
//			 for(Entry<Integer, IndexRule> entry : ruleMap.entrySet()) {
//				 System.out.println(entry.getValue() + "Par:\t" +entry.getValue().getParentIndices()+ "\tdel: "+entry.getValue().deleteGraph);
//			 }
			 for(Integer rNr : ruleMap.keySet()) {
					
				 Set<String> nts = buildNTriples(rNr, new HashSet<Integer>(), new HashSet<Integer>());
//				 System.out.println("Building rule nr "+rNr+": "+nts);
				
				 triples.addAll(nts);
			 };
		}
		System.out.println("\n\nNr of triples: "+triples.size());

		
		
		/*############################################### VALUE MODEL  ####################################*/
		System.out.println("Reading valueModel");
		setChanged();
		status.update("Triples successfully built", "Reading value model");
 		notifyObservers(status);		
		
		Model valueModel = ModelFactory.createDefaultModel();
		if(fileMap.containsKey(BasicCompressor.valueModelFileName)) {
			valueModel = ModelLoader.getModel(fileMap.get(BasicCompressor.valueModelFileName).getAbsolutePath());
		}
		if(fileMap.containsKey(ModelCompressor.valueModelHDTName)) {
			valueModel = parseHDTValueModel(fileMap.get(ModelCompressor.valueModelHDTName));
		}
			
		StmtIterator it = valueModel.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			String s = stmt.getSubject().getURI().substring(stmt.getSubject().getURI().lastIndexOf("/")+1);
			String p = stmt.getPredicate().getURI().substring(stmt.getSubject().getURI().lastIndexOf("/")+1);
			RDFNode o = stmt.getObject();
			Resource subj = globalModel.getResource(subjects.get(Integer.parseInt(s)));
			Property prop = globalModel.getProperty(properties.get(Integer.parseInt(p)));
			Statement statement = globalModel.createLiteralStatement(subj, prop, o.asLiteral());
			globalModel.add(statement);
			System.out.println("Read Stmt: " + subj+ " " + prop +" "+o);
		}
		
		setChanged();
		status.setFinished();
		status.update("Successfully decompressed " + triples.size() + " triples! Size of global Model:"+globalModel.size(), "");
 		notifyObservers(status);
// 		Collections.sort(triples);
// 		for(String s : triples) {
// 			System.out.println(s);
// 		}
 		StmtIterator stmtIt = globalModel.listStatements();
 		while(stmtIt.hasNext()) {
 			
 			System.out.println(stmtIt.next().asTriple());
 		}
 		System.out.println("Decompression finished. Model with "+globalModel.size()+" Statements loaded.");
		return globalModel;
	}

	/**
	 * Builds the RDF Model once all Rules are read.
	 * @param ruleNr current Rule to extract
	 * @param uris Set of Integers of children rules pointing to Rule ruleNr
	 * @param vistitedParentNodes Set of already vistited parents to avoid circles: neccessary for rules with delete graph
	 * @return
	 * @throws IOException
	 */
	private Set<String> buildNTriples(int ruleNr, Set<Integer> uris, Set<Integer> vistitedParentNodes) throws IOException {
		HashSet<String> triples = new HashSet<String> ();
		if(vistitedParentNodes.contains(ruleNr))
			return triples;
		vistitedParentNodes.add(ruleNr);
		IndexRule r = ruleMap.get(ruleNr);
		if(uris.isEmpty()) {
			for(Integer sID : r.getProfile().getSubjects()) {
				String triple = subjects.get(sID) + " " 
						+ properties.get(r.getProfile().getProperty()) + " "
						+ subjects.get(r.getProfile().getObject()) +" .";
				Statement statement = globalModel.createStatement(globalModel.getResource(subjects.get(sID)),
						globalModel.getProperty(properties.get(r.getProfile().getProperty())),
						globalModel.getResource(subjects.get(r.getProfile().getObject())));
				globalModel.add(statement);
//				System.out.println("Building triple: "+triple);
				triples.add(triple);
			}
			for(Integer parentID : r.getParentIndices()) { //recursion
//				System.out.println("Recursion on "+parentID+" with "+r.getProfile().getSubjects());
//				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
//			    String s = bufferRead.readLine();
				triples.addAll(buildNTriples(parentID, r.getProfile().getSubjects(), vistitedParentNodes));
			}
		} else { // build triples from uris from children rules.
			for(Integer sID : uris) {
				if(!r.deleteGraph.contains(sID)) {
					String triple = subjects.get(sID) + " " 
							+ properties.get(r.getProfile().getProperty()) + " "
							+ subjects.get(r.getProfile().getObject()) +" .";
					Statement statement = globalModel.createStatement(globalModel.getResource(subjects.get(sID)),
							globalModel.getProperty(properties.get(r.getProfile().getProperty())),
							globalModel.getResource(subjects.get(r.getProfile().getObject())));
					globalModel.add(statement);
					triples.add(triple);
				}
			}
			uris.removeAll(r.deleteGraph);
			if(uris.size()>0)
				for(Integer parentID : r.getParentIndices()) { //recursion
	//				uris.removeAll(r.deleteGraph);
//					System.out.println("Recursion using delete graph on parent "+parentID+" with Uris "+uris);
//					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
//				    String s = bufferRead.readLine();
					triples.addAll(buildNTriples(parentID, uris, vistitedParentNodes));
				}
		}
		return triples;
	}
	
	/**
	 * Iterates once over all rules and adds all subjects to its parents.
	 */
	@Deprecated
	private void addSubjectsToSuperRules() {
		for(Entry<Integer, IndexRule> entry : ruleMap.entrySet()) {
			Set<Integer> subs = entry.getValue().getProfile().getSubjects();
			if(subs.size()>0) {//for all subjects
				for(Integer parIndex : entry.getValue().getParentIndices()) {//for each parent
					IndexRule parent = ruleMap.get(parIndex);
					Set<Integer> addSubs = new HashSet<Integer>();
					addSubs.addAll(subs);
					addSubs.removeAll(parent.deleteGraph);
					for(Integer s : addSubs) {
						parent.getProfile().addSubject(s);
					}
//					System.out.println("Added "+addSubs+" to "+parent);
				}
			}
		}
	}
	
	
	
/* ###############################################################
 * 				Parsing methods
 * ###############################################################
 * */
	
	/**
	 * Method parses line of URI prefix definitions.
	 * @param line
	 */
	private void parseAbbreviations(String line) {
		String abr[] = line.split("\\|");
		globalModel.setNsPrefix(abr[0], abr[1]);
	}
	/**
	 * Method to parse a line of the subject/object dictionary.
	 * @param line
	 */
	private void parseSubjects(String line) {
		subjects.put(subjects.size(), line);
		globalModel.createResource(line);
	}
	/**
	 * Method parses line of property dictionary.
	 * @param line
	 */
	private void parseProperties(String line) {
		properties.put(properties.size(), line);
		globalModel.createProperty(line);
	}
	/**
	 * Central decompression method to parse a line of serialized rules. 
	 * @param line Line to parse.
	 * @param prop_before number of the property of the rule of the line before.
	 * @param ruleNr Number of this rule which is the number of the line in the serialized rules section.
	 * @return The number of the property for this rule.
	 */
	private int parseRule (String line, int prop_before, int ruleNr) {
		int propNr = prop_before;
		int split1 = -1;
		if(line.indexOf("{") != -1) {
			split1 = line.indexOf("{");
		} else {
			if(line.indexOf("[") != -1)
				split1 = line.lastIndexOf("[");
			else
				if(line.indexOf("(") != -1) {
					split1 = line.lastIndexOf("(");
				}
		}
		String po = line;
		String rest = "";
	
		if(split1 != -1) {
			po = line.substring(0, split1);
			rest = line.substring(split1);
		}
		String[] parts = po.split("-");
		IndexProfile profile;

		if(parts.length == 2) {
			profile = new IndexProfile(new Integer(parts[0]), new Integer(parts[1]));
			propNr = new Integer(parts[0]);
		}
		else {
			profile = new IndexProfile(propNr, new Integer(parts[0]) );
		}
		IndexRule rule = new IndexRule(profile);
		rule.setNumber(ruleNr);
		if(rest.length()>0) {
			int supers = line.indexOf("[");
				//parse subs
				String substr = rest;
				if(supers != -1) {
					if( rest.indexOf("[") != -1)
						substr = rest.substring(0, rest.indexOf("["));
					else
						substr = rest;
				}
				else 
					if(rest.indexOf("(")!= -1)
						substr = rest.substring(0, rest.indexOf("("));
					else 
					substr = rest;
				if(substr.length()>0) {
					substr = substr.substring(1);
					String[] subjects = substr.split("\\|");
					int offset = 0;
					for(String s : subjects) {
						profile.addSubject(offset+Integer.parseInt(s));
						offset += Integer.parseInt(s);
					}
				}
			
			if(supers != -1) {
				//parsing super rules
				String superstr = line.substring(supers+1);
				if(line.indexOf("(")!=-1)
					superstr = line.substring(supers+1, line.indexOf("("));
			
				
//				System.out.println("parsing supers:"+superstr);
				String[] rulesStr = superstr.split("\\|");
				int offset = 0;
				for(String s: rulesStr) {
					rule.addParentIndex(offset+new Integer(s));
					offset += new Integer(s);
				}
			}	
			
		} //rest>0
		int d = line.indexOf("(");
		if(d != -1) {
			hasDeleteRules = true;
			String del = line.substring(d+1);
			String[] subjects = del.split("\\|");
			int offset = 0;
			for(String s : subjects) {
				rule.deleteGraph.add(offset+new Integer(s));
				offset+=new Integer(s);
			}
		}
		ruleMap.put(ruleNr, rule);
		return propNr;		
	}
	
	private static BufferedReader getBufferedReaderForRuleFile(File fileIn) throws FileNotFoundException, CompressorException {
	    FileInputStream fin = new FileInputStream(fileIn);
	    BufferedReader br2 = new BufferedReader(new InputStreamReader(fin));
	    return br2;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException, ArchiveException, CompressorException {
		File file = new File("resources/runningExample.n3.cp.tar.bz2");

		DefaultDecompressor decmpr = new DefaultDecompressor(file);
		try {
			Model glob = decmpr.decompress(file);
//			Map<String,String> map = glob.getNsPrefixMap();
//			for(Entry<?, ?> e : map.entrySet())
//				System.out.println("@prefix "+ e.getKey()+" "+"<"+e.getValue()+">");
//			System.out.println("---"+glob.size());
			StmtIterator it = glob.listStatements();
			while(it.hasNext()) {
				Statement stmt = it.next();
//				System.out.println(stmt);
//				System.out.println(stmt.getSubject().getLocalName() + " " + stmt.getPredicate()+" " +stmt.getObject());
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CompressorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** Untar an input file into an output file.

	 * The output file is created in the output folder, having the same name
	 * as the input file, minus the '.tar' extension. 
	 * 
	 * @param inputFile     the input .tar file
	 * @param outputDir     the output directory file. 
	 * @throws IOException 
	 * @throws FileNotFoundException
	 *  
	 * @return  The {@link List} of {@link File}s with the untared content.
	 * @throws ArchiveException 
	 * @throws CompressorException 
	 */
	private static Map<String, File> unTar(final File inputFile, final File outputDir) throws FileNotFoundException, IOException, ArchiveException, CompressorException {
		Map<String, File> fileMap = new HashMap<String, File>();
	    FileInputStream fin = new FileInputStream(inputFile);
	    BufferedInputStream bis = new BufferedInputStream(fin);
	    CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream("bzip2", bis);
	  
	    final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", input);
	    TarArchiveEntry entry = null; 
	    while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
	        final File outputFile = new File(outputDir, entry.getName());
	        if (entry.isDirectory()) {
	            if (!outputFile.exists()) {
	              if (!outputFile.mkdirs()) {
	                    throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
	                }
	            }
	        } else {
	            final OutputStream outputFileStream = new FileOutputStream(outputFile, false); 
	            IOUtils.copy(debInputStream, outputFileStream);
	            outputFileStream.close();
	        }
	        fileMap.put(entry.getName(), outputFile);
	    }
	    debInputStream.close(); 
	    return fileMap;
	}
	
	/**
	 * Method to parse valueModel out of HDT file.
	 * @param hdtFile untared .hdt File
	 * @return JENA Model for HDT File
	 * @throws IOException
	 */
	private Model parseHDTValueModel(File hdtFile) throws IOException {
		// Load HDT file using the hdt-java library
		System.out.println("Loading HDT of file "+hdtFile.getName());
		HDT hdt = HDTManager.mapIndexedHDT(hdtFile.getAbsolutePath(), null);
		 
		// Create Jena Model on top of HDT.
		HDTGraph graph = new HDTGraph(hdt);
		System.out.println("Loading Model for HDT");
		return ModelFactory.createModelForGraph(graph);
	}

}
