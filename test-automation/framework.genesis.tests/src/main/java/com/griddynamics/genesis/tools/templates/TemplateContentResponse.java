package com.griddynamics.genesis.tools.templates;

public class TemplateContentResponse extends Template {
     String content;

	public TemplateContentResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TemplateContentResponse(String name, String version) {
		super(name, version);
		// TODO Auto-generated constructor stub
	}

	public TemplateContentResponse(String content) {
		super();
		this.content = content;
	}

	public TemplateContentResponse(String name, String version, String content) {
		super(name, version);
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((content == null) ? 0 : content.hashCode());
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
		TemplateContentResponse other = (TemplateContentResponse) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TemplateContentResponse [content=" + content + "]";
	}
     
     
}
