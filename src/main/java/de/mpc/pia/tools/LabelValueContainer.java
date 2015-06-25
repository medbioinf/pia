package de.mpc.pia.tools;

/**
 * A simple container to hold a value T with a String label.
 * @author julian
 *
 * @param <T>
 */
public class LabelValueContainer<T> {
	
	private String label;
	private T value;
	
	public LabelValueContainer(String label, T value) {
		this.label = label;
		this.value = value;
	}
	
	public String getLabel() {
		return label;
	}
	
	public T getValue() {
		return value;
	}
}