package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import de.uni_leipzig.simba.data.DefaultCompressedGraph;
import de.uni_leipzig.simba.data.Profile;
import de.uni_leipzig.simba.data.Rule;
    
public class DefaultCompressor implements Compressor {

    public DefaultCompressor(){
    }
    
    public void compress(File input){
	Model model = FileManager.get().loadModel( input.toString() );

	StringWriter graphOutput = new StringWriter();
	model.write(graphOutput, "TURTLE");
	System.out.println(graphOutput);

	// build inverse list of p/o tuples
	DefaultCompressedGraph dcg = new DefaultCompressedGraph();

	StmtIterator iter = model.listStatements();
	while( iter.hasNext() ){
	    Statement stmt = iter.next();
	    Profile profile = new Profile(stmt.getPredicate(),
					  stmt.getObject());
	    profile.addSubject(stmt.getSubject());
	    Rule rule = new Rule(profile);

	    dcg.addRule(rule);
	}
	System.out.println("\nCompressed graph:\n"+dcg);
	
	// (build addgraph)

	// serialize inverse list
	String output = dcg.serialize();
	System.out.println("Serialized compressed graph:\n" + output);

	// compress with bzip2
	try{
	    OutputStream os = new FileOutputStream(input.getAbsolutePath() + ".bz2");
	    OutputStream bzos = new BZip2CompressorOutputStream(os);
	    bzos.write(output.getBytes());
	    bzos.close();
	    os.close();
	}
	catch (IOException ioe){
	    System.out.println(ioe);
	}
    }
}
