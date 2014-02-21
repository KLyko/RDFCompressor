package de.uni_leipzig.simba.data;

import java.util.List;

public interface IRule<IPRofile> {

	public int getNumber();
	public void setNumber(int nr);

	public void setProfile(IPRofile profile);
	
	public void addParent(IRule<IPRofile> r);

    public IPRofile getProfile();
    
    public List<IRule<IPRofile>> getParents();
}
