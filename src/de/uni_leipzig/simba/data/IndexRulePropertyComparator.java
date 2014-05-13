package de.uni_leipzig.simba.data;

import java.util.Comparator;

/**
 * Comparator to sort IndexRules based upon there Property.
 * @author Klaus Lyko
 *
 */
public class IndexRulePropertyComparator implements Comparator<IndexRule>{

	@Override
	public int compare(IndexRule o1, IndexRule o2) {
//		if(o1.isAtomic()) {
//			if(o2.isAtomic())
//				return o1.profile.prop.compareTo(o2.profile.prop);
//			else
//				return -1;
//		} else {
//			if(o2.isAtomic())
//				return 1;
//			else
//				o1.profile.prop.compareTo(o2.profile.prop);
//		}
		return o1.profile.prop.compareTo(o2.profile.prop);
	}

}
