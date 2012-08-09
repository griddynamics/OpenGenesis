package com.griddynamics.genesis.tools.templates;

public class Variable {
	String name;
    String description;
    Boolean optional;
    String defaultValue;
	
    
    public Variable() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Variable(String name, String description, Boolean optional,
			String defaultValue) {
		super();
		this.name = name;
		this.description = description;
		this.optional = optional;
		this.defaultValue = defaultValue;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public Boolean getOptional() {
		return optional;
	}


	public void setOptional(Boolean optional) {
		this.optional = optional;
	}


	public String getDefaultValue() {
		return defaultValue;
	}


	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((optional == null) ? 0 : optional.hashCode());
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
		Variable other = (Variable) obj;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (optional == null) {
			if (other.optional != null)
				return false;
		} else if (!optional.equals(other.optional))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Variable [name=" + name + ", description=" + description
				+ ", optional=" + optional + ", defaultValue=" + defaultValue
				+ "]";
	}
    
    
    
}
