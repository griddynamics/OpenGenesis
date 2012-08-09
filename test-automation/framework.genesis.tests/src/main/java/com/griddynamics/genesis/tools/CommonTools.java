package com.griddynamics.genesis.tools;

/**
 * Class containing utility methods used in JBehave steps
 * 
 * @author ybaturina
 *
 */
public class CommonTools {

	/**
	 * Method converts String to String array. Designed to 
	 * treat situations properly when initial string is nullable or empty
	 * 
	 * @param val
	 * @return
	 */
	public static String[] processStringValue(String val) {
		if (val == null)
			return null;
		else if (val.equals(""))
			return new String[0];
		else
			return val.split(", ");
	}
}
