RDFCompressor
=============

Project to compress RDF data. The idea is to avoid redundant triples (e.g. <s1, p, o>,<s2, p, o>,.., <sn, p, o>) 
by learning rules to be applied on to sub graphs (which are essentially sets of subjects). These rules we will replace
the triples and will produce the same triples on decompression. 

This Project is a research prototype. Developed by the AKSW research group at the University of Leipzig: http://aksw.org/About.html
