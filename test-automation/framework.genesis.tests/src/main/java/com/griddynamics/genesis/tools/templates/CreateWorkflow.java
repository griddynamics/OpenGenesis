package com.griddynamics.genesis.tools.templates;

import java.util.Arrays;

public class CreateWorkflow {
     String name;
     Variable[] variable;

     
     public CreateWorkflow() {
		super();
		// TODO Auto-generated constructor stub
	}


	public CreateWorkflow(String name, Variable[] variable) {
		super();
		this.name = name;
		this.variable = variable;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Variable[] getVariable() {
		return variable;
	}


	public void setVariable(Variable[] variable) {
		this.variable = variable;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(variable);
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
		CreateWorkflow other = (CreateWorkflow) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(variable, other.variable))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "CreateWorkflow [name=" + name + ", variable="
				+ Arrays.toString(variable) + "]";
	}
     
     
}
