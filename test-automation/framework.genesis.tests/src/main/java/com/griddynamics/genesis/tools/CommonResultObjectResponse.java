package com.griddynamics.genesis.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Class containing fields as in common result object returning for most REST changing operations
 * 
 * @author ybaturina
 *
 */
public class CommonResultObjectResponse {

	public boolean isSuccess = false;
	public boolean isNotFound = false;
	public Map<String, String> serviceErrors = new HashMap<String, String>();
	public Map<String, String> variablesErrors = new HashMap<String, String>();
	public String[] compoundServiceErrors = new String[0];
	public String[] compoundVariablesErrors = new String[0];

	public static CommonResultObjectResponse getResultFromString(
			boolean isSuccess, boolean isNotFound, String serviceErrors,
			String variablesErrors, String compoundServiceErrors,
			String compoundVariablesErrors) {
		Gson gson = new Gson();
		String objToString = "{\"serviceErrors\":{%s},\"variablesErrors\":{%s},\"compoundServiceErrors\":[%s],\"compoundVariablesErrors\":[%s],\"isNotFound\":%s,\"isSuccess\":%s}";
		return gson.fromJson(String.format(objToString, serviceErrors,
				variablesErrors, compoundServiceErrors,
				compoundVariablesErrors, isNotFound, isSuccess),
				CommonResultObjectResponse.class);
	}

	public CommonResultObjectResponse() {

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(compoundServiceErrors);
		result = prime * result + Arrays.hashCode(compoundVariablesErrors);
		result = prime * result + (isNotFound ? 1231 : 1237);
		result = prime * result + (isSuccess ? 1231 : 1237);
		result = prime * result
				+ ((serviceErrors == null) ? 0 : serviceErrors.hashCode());
		result = prime * result
				+ ((variablesErrors == null) ? 0 : variablesErrors.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommonResultObjectResponse other = (CommonResultObjectResponse) obj;
		if (!Arrays.equals(compoundServiceErrors, other.compoundServiceErrors))
			return false;
		if (!Arrays.equals(compoundVariablesErrors,
				other.compoundVariablesErrors))
			return false;
		if (isNotFound != other.isNotFound)
			return false;
		if (isSuccess != other.isSuccess)
			return false;
		if (serviceErrors == null) {
			if (other.serviceErrors != null)
				return false;
		} else if (!serviceErrors.equals(other.serviceErrors))
			return false;
		if (variablesErrors == null) {
			if (other.variablesErrors != null)
				return false;
		} else if (!variablesErrors.equals(other.variablesErrors))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CommonResultObjectResponse [isSuccess=" + isSuccess
				+ ", isNotFound=" + isNotFound + ", serviceErrors="
				+ serviceErrors + ", variablesErrors=" + variablesErrors
				+ ", compoundServiceErrors="
				+ Arrays.toString(compoundServiceErrors)
				+ ", compoundVariablesErrors="
				+ Arrays.toString(compoundVariablesErrors) + "]";
	}

}
