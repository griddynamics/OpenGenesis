package com.griddynamics.genesis.tools.templates;

public class TemplateDescriptionResponse extends Template{
    CreateWorkflow createWorkflow;

	public TemplateDescriptionResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TemplateDescriptionResponse(String name, String version) {
		super(name, version);
		// TODO Auto-generated constructor stub
	}

	public TemplateDescriptionResponse(CreateWorkflow createWorkflow) {
		super();
		this.createWorkflow = createWorkflow;
	}

	
	public TemplateDescriptionResponse(String name, String version,
			CreateWorkflow createWorkflow) {
		super(name, version);
		this.createWorkflow = createWorkflow;
	}

	public CreateWorkflow getCreateWorkflow() {
		return createWorkflow;
	}

	public void setCreateWorkflow(CreateWorkflow createWorkflow) {
		this.createWorkflow = createWorkflow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((createWorkflow == null) ? 0 : createWorkflow.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TemplateDescriptionResponse other = (TemplateDescriptionResponse) obj;
		if (createWorkflow == null) {
			if (other.createWorkflow != null)
				return false;
		} else if (!createWorkflow.equals(other.createWorkflow))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TemplateDescriptionResponse [createWorkflow=" + createWorkflow
				+ "]";
	}
    
    
}
