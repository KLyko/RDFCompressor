package de.uni_leipzig.simba.util;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.enums.RDFNotation;
public class HDTEvaluation {
    public static void main(String[] args) throws Exception {
        // Configuration variables
        String baseURI = "http://data.archiveshub.ac.uk/";
        String rdfInput = "resources/archive_hub_dump.nt";
        String inputType = "ntriples";
        String hdtOutput = "resources/archive_hub_dump.hdt";
 
        // Create HDT from RDF file
        HDT hdt = HDTManager.generateHDT(
                            rdfInput,         // Input RDF File
                            baseURI,          // Base URI
                            RDFNotation.parse(inputType), // Input Type
                            new HDTSpecification(),   // HDT Options
                            null              // Progress Listener
                );
//        HDTManager.
        // OPTIONAL: Add additional domain-specific properties to the header:
        //Header header = hdt.getHeader();
        //header.insert("myResource1", "property" , "value");
 
        // Save generated HDT to a file
        hdt.saveToHDT(hdtOutput, null);
    }
}
