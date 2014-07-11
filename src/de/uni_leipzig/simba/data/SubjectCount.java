package de.uni_leipzig.simba.data;

public class SubjectCount implements Comparable{
	public int nr;
	public int count = 1;
	public int new_number;
	public boolean isSubject = true;
	
	public SubjectCount(int nr) {
		this.nr = nr;
		this.new_number = nr;
	}
	
	public SubjectCount(int nr, boolean isSubject) {
		this.nr = nr;
		this.new_number = nr;
		this.isSubject = isSubject;
	}
	
	
	@Override
	public int compareTo(Object o) {
		SubjectCount other = (SubjectCount) o;
		return other.count - this.count;
	}
	@Override
	public int hashCode() {
		return nr;
	}
	
	@Override
	public String toString() {
		return ""+nr;
	}
}
