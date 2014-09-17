package de.uni_leipzig.simba.io;


public class Status {
	String feedback; // for any label presenting some feedback about work done.
	String status; // describes the current operation; 
	int step; // number of this
	int maxSteps = 6;
	public Status(String status, int nr) {
		this.status = status;
		this.step = nr;
	}
	/**
	 * Updates Status with new information and increases its enumerator.
	 * @param feedback
	 * @param status
	 */
	public void update(String feedback, String status) {
		step++;
		this.feedback = feedback;
		this.status = status;
	}
	
	public Status(String status, int nr, int maxSteps) {
		this(status, nr);
		this.maxSteps = maxSteps;
	}
	/**
	 * Is the number of this status the final step;
	 * @return Whether computation is done.
	 */
	public boolean isFinished() {
		return step>=maxSteps;
	}		
}