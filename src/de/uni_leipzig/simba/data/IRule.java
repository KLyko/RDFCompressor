package de.uni_leipzig.simba.data;

import java.util.List;

public interface IRule<IPRofile> {

	public int getNumber();
	public void setNumber(int nr);

	public void setProfile(IPRofile profile);
	
	public void addParent(IRule<IPRofile> parent);

	public void addChild(IRule<IProfile> child);
	
    public IPRofile getProfile();
    
    public List<IRule<IPRofile>> getParents();
    
    public List<IRule<IProfile>> getChildren();
}
