package de.uni_leipzig.simba.decompress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_leipzig.simba.compress.IndexBasedCompressor;
import de.uni_leipzig.simba.data.IndexProfile;
import de.uni_leipzig.simba.data.IndexRule;

public class DefaultDecompressor implements DeCompressor{
	HashMap<Integer, String> subjects = new HashMap<Integer, String>();
	HashMap<Integer, String> properties = new HashMap<Integer, String>();
	HashMap<String, String> abbrev = new HashMap<String, String>();
	
	HashMap<Integer, IndexRule> ruleMap = new HashMap<Integer, IndexRule>();
	
	@Override
	public File decompress(File file) throws IOException {
//		InputStream in = new 
//		
		BufferedReader br = new BufferedReader(new FileReader(file));
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
		 for(Entry<Integer, IndexRule> e: ruleMap.entrySet()) {
			System.out.println(e.getKey()+": "+e.getValue()+" "+e.getValue().getParentIndices());
		 }
		 
		 ArrayList<String> triples = new ArrayList<String>(22);
		 
		 
		 for(Integer rNr : ruleMap.keySet()) {
		
			 Set<String> nts = buildNTriples(rNr, new HashSet<Integer>());
			 System.out.println("Building rule nr "+rNr+": "+nts);
			
			 triples.addAll(nts);
		 }
		 Collections.sort(triples);
		 for(String s : triples) {
			 System.out.println(s);
		 }
		
		System.out.println("\n\nNr of triples: "+triples.size());
		
		
		return null;
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
	}
	
	private void parseProperties(String line) {
//		String[] parts = line.split("\\|");
//		System.out.println(parts[1]+"=>"+parts[0]);
//		properties.put(Integer.parseInt(parts[1]), parts[0]);
		properties.put(properties.size(), line);
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
	
	

}
