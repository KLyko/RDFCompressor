package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
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
	

	@Override
	public void writeIndexFiles() {
		// TODO Auto-generated method stub
		System.out.println("short-to-uri\n====="+shortToUri);
		System.out.println("subjectMap\n====="+subjectMap);
		System.out.println("predicateMap\n====="+propertyMap);
		System.out.println("objectMap\n====="+objectMap);
	}

	 public void compress(File input) {
			Model model = FileManager.get().loadModel( input.toString() );
		
			StringWriter graphOutput = new StringWriter();
			model.write(graphOutput, "TURTLE");
			System.out.println(graphOutput);
			
			shortToUri.putAll(model.getNsPrefixMap());
			
			// build inverse list of p/o tuples
			IndexCompressedGraph dcg = new IndexCompressedGraph();
		
			StmtIterator iter = model.listStatements();
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
				
				System.out.println(s + " -- " + p + " -- " + o);
				int indexS = addIndex(s, SPO.SUBJECT);
				int indexP = addIndex(p, SPO.PREDICATE);
				int indexO = addIndex(o, SPO.OBJECT);
				IndexProfile profile = new IndexProfile(indexP, indexO);
				profile.addSubject(indexS);
				IndexRule rule = new IndexRule(profile);
				dcg.addRule(rule);
			}
			
			dcg.computeSuperRules();
			System.out.println("\nCompressed graph:\n"+dcg);

			// serialize prefixes and put them to the archive
			String prefixes = "";
			for (Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
				prefixes += entry.getKey() + "|" + entry.getValue() + "\n";
			}
			prefixes += "\n";

			// String output = dcg.serialize();
			// System.out.println("Serialized compressed graph:\n" + output);

			String ruleString = "";
			for(IndexRule rule : dcg.getRules()) {
				String p = propertyMap.get(rule.getProfile().getProperty());
				String o = objectMap.get(rule.getProfile().getObject());
				try{int i = Integer.parseInt(o);
					o = subjectMap.get(i);
				} catch(NumberFormatException e){}
				ruleString += ""+rule.getNumber() +": "+ p +"-"+o+" [";
					for(int s : rule.getProfile().getSubjects()) {
						ruleString+=subjectMap.get(s)+" | ";
					}
				ruleString +="] ";
				ruleString+=" {";
				for(IRule sr : rule.getParents()) {
					ruleString += sr.getNumber() +"|";
				}
				ruleString+="}\n";
			}

			// write archive files and bzip it
			String tDir = System.getProperty("java.io.tmpdir");
			
			try{
			    OutputStream os = new FileOutputStream(input.getAbsolutePath() + ".tar.bz2");
			    OutputStream bzos = new BZip2CompressorOutputStream(os);
			    TarArchiveOutputStream aos = new TarArchiveOutputStream(bzos);

			    // write prefixes
			    OutputStream osPrefix = new FileOutputStream(tDir + "prefixes");
			    osPrefix.write(prefixes.getBytes());
			    osPrefix.close();
			    File filePrefix = new File(tDir + "prefixes");
			    TarArchiveEntry entry = new TarArchiveEntry(filePrefix, "prefixes");
			    entry.setSize(filePrefix.length());
			    aos.putArchiveEntry(entry);
			    IOUtils.copy(new FileInputStream(filePrefix), aos);
			    aos.closeArchiveEntry();

			    // write subject index
			    OutputStream osSubject = new FileOutputStream(tDir + "subjects");
			    osSubject.write(this.subjectMap.toString().getBytes());
			    osSubject.close();
			    File fileSubject = new File(tDir + "subjects");
			    entry = new TarArchiveEntry(fileSubject, "subjects");
			    entry.setSize(fileSubject.length());
			    aos.putArchiveEntry(entry);
			    IOUtils.copy(new FileInputStream(fileSubject), aos);
			    aos.closeArchiveEntry();

			    // write object index
			    OutputStream osObject = new FileOutputStream(tDir + "objects");
			    osObject.write(this.objectMap.toString().getBytes());
			    osObject.close();
			    File fileObject = new File(tDir + "objects");
			    entry = new TarArchiveEntry(fileObject, "objects");
			    entry.setSize(fileObject.length());
			    aos.putArchiveEntry(entry);
			    IOUtils.copy(new FileInputStream(fileObject), aos);
			    aos.closeArchiveEntry();

			    // write property index
			    OutputStream osProperty = new FileOutputStream(tDir + "properties");
			    osProperty.write(this.propertyMap.toString().getBytes());
			    osProperty.close();
			    File fileProperty = new File(tDir + "properties");
			    entry = new TarArchiveEntry(fileProperty, "properties");
			    entry.setSize(fileProperty.length());
			    aos.putArchiveEntry(entry);
			    IOUtils.copy(new FileInputStream(fileProperty), aos);
			    aos.closeArchiveEntry();

			    // write rules
			    OutputStream osRule = new FileOutputStream(tDir + "rules");
			    System.out.println("#######"+ruleString);
			    osRule.write(ruleString.getBytes());
			    osRule.close();
			    File fileRule = new File(tDir + "rules");
			    entry = new TarArchiveEntry(fileRule, "rules");
			    entry.setSize(fileRule.length());
			    aos.putArchiveEntry(entry);
			    IOUtils.copy(new FileInputStream(fileRule), aos);
			    aos.closeArchiveEntry();
			    
			    aos.finish();
			    aos.close();
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
