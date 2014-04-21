package de.uni_leipzig.simba.data;

import java.util.HashMap;

/**
 * Class to Handle a map of rules.
 * Basically holds a Map < prop => Map <object> => ruleNr >
 * @author Klaus Lyko
 *
 */
public class RuleMap {
	HashMap<Integer, HashMap<Integer, IndexRule>> ruleMap;
	int size = 0;
}
