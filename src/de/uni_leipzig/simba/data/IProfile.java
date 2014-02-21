package de.uni_leipzig.simba.data;

import java.util.Set;

public interface IProfile<S, P, O> {
	public void setObject(O object);
	public void setProperty(P property);
	public int size();
	public boolean addSubject(S subject);

	public P getProperty();
	public O getObject();
	public Set<S> getSubjects();
}
