package de.uni_leipzig.simba.data;

import java.util.Comparator;

/**
 * Comparator to sort IndexRules based upon there Property.
 * @author Klaus Lyko
 *
 */
public class IndexRuleComparator implements Comparator<IndexRule>{

	@Override
	public int compare(IndexRule o1, IndexRule o2) {
		return o1.profile.prop.compareTo(o2.profile.prop);
	}

}
