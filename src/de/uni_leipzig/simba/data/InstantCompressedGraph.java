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

	public void addRule(IndexRule r, Set<Integer> subs) {
		r.nr = rules.size();
		computeSuperRules(r);
		rules.add(r);		
	}
	
	
	public void computeSuperRules(IndexRule r) {
		for(int i = 0; i<rules.size(); i++) {//for every rule
			IndexRule o = rules.get(i);
			if(r.profile.size() >= o.profile.size()) {
				// r is a possible super rule
				if(r.profile.min <= o.profile.min)
					if(r.profile.max >= o.profile.max)
//						if(!o.parents.contains(r)) // neccesary, even possible?
							if(r.profile.subjects.containsAll(o.profile.subjects)) {
								System.out.println("Found children: "+o+"  < "+r);
								o.parents.add(r);
								r.children.add(o);
							}
			} else { // other is a potential super rule
				if(o.profile.min <= r.profile.min)
					if(o.profile.max >= r.profile.max)
						if(!r.parents.contains(o))
							if(o.profile.subjects.containsAll(r.profile.subjects)) {
								r.parents.add(o);
								o.children.add(r);
								r.parents.addAll(o.parents);
							}
			}
			r.superRulesComputed = true;
		}
	}
	
	
	/**
	 * Checks for present superrules or subRules for this new Rule.
	 * @param r
	 */
	@Deprecated
	public void checkForSuperRules(IndexRule r) {
//		if(r.getProfile().size()>1)
			for(int i = 0; i<rules.size(); i++) {
				IndexRule o = rules.get(i);
				if(isSuperRule(o, r)) {
					r.addParent(o);
					o.addChild(r);
//					return;
				} else
					if(isSuperRule(r, o)) {
						o.addParent(r);
						r.addChild(o);
//						return;
					}
			}
	}
	/**
	 * Checks if super is a superrule of sup, that is if i contains all subjects
	 * @param sub
	 * @return
	 */
	@Deprecated
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
