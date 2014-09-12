package de.uni_leipzig.simba.decompress;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;
import de.uni_leipzig.simba.io.ModelLoader;

public class DefaultDecompressor implements DeCompressor{
	HashMap<Integer, String> subjects = new HashMap<Integer, String>();
	HashMap<Integer, String> properties = new HashMap<Integer, String>();
	HashMap<String, String> abbrev = new HashMap<String, String>();
	
	HashMap<Integer, IndexRule> ruleMap = new HashMap<Integer, IndexRule>();
	

	Model globalModel = ModelFactory.createDefaultModel();
	
	@Override
	public Model decompress(File file) throws IOException, CompressorException {

		
		BufferedWriter bw = new BufferedWriter(new FileWriter("tmp.n3", false));
		BufferedReader br = getBufferedReaderForBZ2File(file.getAbsolutePath());
		
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
				 	case 4: bw.write(line); bw.newLine(); break;
				 }
			 }
		 }
		 // structure loaded
//		 for(Entry<Integer, IndexRule> e: ruleMap.entrySet()) {
//			System.out.println(e.getKey()+": "+e.getValue()+" "+e.getValue().getParentIndices());
//		 }
//		 
		 ArrayList<String> triples = new ArrayList<String>(22);
		 
		 
		 for(Integer rNr : ruleMap.keySet()) {
		
			 Set<String> nts = buildNTriples(rNr, new HashSet<Integer>());
//			 System.out.println("Building rule nr "+rNr+": "+nts);
			
			 triples.addAll(nts);
		 }
		 Collections.sort(triples);
		 for(String s : triples) {
//			 String tr = s.
//			 globalModel.createStatement(globalModel.getResource(uri), p, o)
			 System.out.println(s);
		 }
		
		System.out.println("\n\nNr of triples: "+triples.size());
		
		
		/*############################################### VALUE MODEL  ####################################*/
		System.out.println("Reading valueModel");
		
		bw.close();
		
		
		Model valueModel = ModelFactory.createDefaultModel();
		valueModel = ModelLoader.getModel("tmp.n3");
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
//			System.out.println("Read Stmt: " + subj+ " " + prop +" "+o);
		}
		
		
		return globalModel;
	}

	/**
	 * 
	 * @param ruleNr
	 * @param init
	 * @param uris
	 * @return
	 */
	private Set<String> buildNTriples(int ruleNr, Set<Integer> uris) {
		HashSet<String> triples = new HashSet<String> ();

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
				triples.add(triple);
			}
			for(Integer parentID : r.getParentIndices()) { //recursion
				triples.addAll(buildNTriples(parentID, r.getProfile().getSubjects()));
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
			for(Integer parentID : r.getParentIndices()) { //recursion
				triples.addAll(buildNTriples(parentID, uris));
			}
		}
		return triples;
	}
	
	
/* ###############################################################
 * 				Parsing methods
 * ###############################################################
 * */
	
	
	private void parseAbbreviations(String line) {
		System.out.println("parse Abbr"+line);
	}
	
	private void parseSubjects(String line) {
//		String[] parts = line.split("\\|");
//		System.out.println(parts[1]+"=>"+parts[0]);
//		subjects.put(Integer.parseInt(parts[1]), parts[0]);
		subjects.put(subjects.size(), line);
		globalModel.createResource(line);
	}
	
	private void parseProperties(String line) {
//		String[] parts = line.split("\\|");
//		System.out.println(parts[1]+"=>"+parts[0]);
//		properties.put(Integer.parseInt(parts[1]), parts[0]);
		properties.put(properties.size(), line);
		globalModel.createProperty(line);
	}
	
	private int parseRule (String line, int prop_before, int ruleNr) {
		int propNr = prop_before;
		int split1 = -1;
		if(line.indexOf("{") != -1) {
			split1 = line.indexOf("{");
		} else {
			if(line.indexOf("[") != -1)
				split1 = line.lastIndexOf("[");
			else
				if(line.indexOf("(") != -1)
					split1 = line.lastIndexOf("(");
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
	
	public static BufferedReader getBufferedReaderForBZ2File(String fileIn) throws FileNotFoundException, CompressorException {
	    FileInputStream fin = new FileInputStream(fileIn);
	    BufferedInputStream bis = new BufferedInputStream(fin);
	    CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream("bzip2", bis);
	    BufferedReader br2 = new BufferedReader(new InputStreamReader(input));

	    return br2;
	}
	
	
	public static void main(String args[]) {
		File file = new File("resources/dummy_data3.nt.cp.bz2");
		DefaultDecompressor decmpr = new DefaultDecompressor();
		try {
			Model glob = decmpr.decompress(file);
			System.out.println("---"+glob.size());
			StmtIterator it = glob.listStatements();
			while(it.hasNext()) {
				Statement stmt = it.next();
				System.out.println(stmt);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CompressorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
