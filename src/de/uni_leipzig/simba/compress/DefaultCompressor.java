package de.uni_leipzig.simba.compress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;

import de.uni_leipzig.simba.data.DefaultCompressedGraph;
import de.uni_leipzig.simba.data.Profile;
import de.uni_leipzig.simba.data.Rule;
    
public class DefaultCompressor implements Compressor {

    public DefaultCompressor(){
    }
    
    public void compress(File input){
	Model model = parseInputFile(input);

	StringWriter output = new StringWriter();
	model.write(output, "TURTLE");
	System.out.println(output);

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
	System.out.println(dcg);
	
	// (build addgraph)

	// serialize inverse list
	System.out.println(dcg.serialize());

	try{
	    PrintWriter writer = new PrintWriter("data.txt", "UTF-8");
	    writer.println(dcg.serialize());
	    writer.close();
	} catch (FileNotFoundException fnfe){
	    System.out.println(fnfe);
	}
	catch (UnsupportedEncodingException uee){
	    System.out.println(uee);
	}

	// compress with bzip
    }

    private Model parseInputFile(File input){
	Model m = FileManager.get().loadModel( input.toString() );
	return m;
    }
}
