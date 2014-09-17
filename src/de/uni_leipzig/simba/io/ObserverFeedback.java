package de.uni_leipzig.simba.io;

import de.uni_leipzig.simba.data.IndexCompressedGraph;
/**
 * Class to wrap around the feedback for any Observer.
 * @author Klaus Lyko
 *
 */
public class ObserverFeedback {	
		
	/**Time passed in milliseconds**/
	public double timePassed = 0d;
	/**Current graph**/
	public IndexCompressedGraph graph = null;
	public Status currentStatus;
	public void update(Status status, IndexCompressedGraph graph, double timePassed) {
		this.graph = graph;
		this.currentStatus = status;
		this.timePassed = timePassed;
	}

}



