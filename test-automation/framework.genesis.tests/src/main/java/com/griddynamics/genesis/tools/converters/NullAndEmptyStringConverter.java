package com.griddynamics.genesis.tools.converters;

import java.lang.reflect.Type;

import org.jbehave.core.steps.ParameterConverters.ParameterConverter;

/**
 * Special parameter converter which treats String values in test tables properly
 * 
 * @author ybaturina
 *
 */
public class NullAndEmptyStringConverter implements ParameterConverter {

	public boolean accept(Type type) {
		if (type instanceof Class<?>) {
			return String.class.isAssignableFrom((Class<?>) type);
		}
		return false;
	}

	public Object convertValue(String value, Type type) {
		if (value.equals("empty")) {
			return "";
		}
		if (value.equals("null")) {
			return null;
		}
		return value;
	}

}
