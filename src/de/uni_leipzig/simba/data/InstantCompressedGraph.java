package de.uni_leipzig.simba.data;

import java.util.Set;

/**
 * Compressedgraph holding and managing all rules. If a new Rule is added a check for possible
 * super rules is done over all present rules.
 * @author Klaus Lyko
 *
 */
public class InstantCompressedGraph extends IndexCompressedGraph{

	
	public InstantCompressedGraph(double expRules, boolean useBloom,
			int deleteBorder) {
		super(expRules, useBloom, deleteBorder);
		// TODO Auto-generated constructor stub
	}

	public void addRule(IndexRule r, Set<Integer> subs) throws Exception {
		checkForSuperRules(r);
		rules.add(r);		
	}
	
	/**
	 * Checks for present superrules or subRules for this new Rule.
	 * @param r
	 */
	public void checkForSuperRules(IndexRule r) {
		for(int i = 0; i<rules.size(); i++) {
			IndexRule o = rules.get(i);
			if(isSuperRule(o, r)) {
				r.addParent(o);
				o.addChild(r);
			}
			if(isSuperRule(r, o)) {
				o.addParent(r);
				r.addChild(o);
			}
		}
	}
	/**
	 * Checks if super is a superrule of sup, that is if i contains all subjects
	 * @param sub
	 * @return
	 */
	private boolean isSuperRule(IndexRule superRule, IndexRule rule) {
		IndexProfile superProfile = superRule.getProfile();
		IndexProfile subProfile = rule.getProfile();
		// check for same rule should not be neccesary
			if(superProfile.size()>subProfile.size())
				if(superProfile.min<=subProfile.min)
					if(superProfile.max>=subProfile.max)
						if(superProfile.containsAll(subProfile))
							return true;
		return false;
	}
}
