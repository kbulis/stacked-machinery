package com.unowmo.machinery;

/**
 * Container for command parameters as specified by machine states for each
 * axion (enter or leave). 
 * 
 * @author Kirk Bulis
 *
 */
public class LabeledValuePair {

	public String label = "";
	public String value = "";

	/**
	 * Construct default.
	 * 
	 * @param label initial label
	 * @param value initial value
	 */
	public LabeledValuePair(final String label, final String value) {
		this.label = label;
		this.value = value;
	}

	/**
	 * Construct default.
	 */
	public LabeledValuePair() {
	}

}
