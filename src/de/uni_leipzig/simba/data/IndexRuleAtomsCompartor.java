package de.uni_leipzig.simba.data;

import java.util.Comparator;

public class IndexRuleAtomsCompartor  implements Comparator<IndexRule>{

	@Override
	public int compare(IndexRule o1, IndexRule o2) {
		if(o1.isAtomic()) {
			if(o2.isAtomic())
				return 0;//o1.profile.prop.compareTo(o2.profile.prop);
			else
				return 1;
		} else {
			if(o2.isAtomic())
				return -1;
			else
				return 0;//o1.profile.prop.compareTo(o2.profile.prop);
		}
//		return o1.profile.prop.compareTo(o2.profile.prop);
	}

}