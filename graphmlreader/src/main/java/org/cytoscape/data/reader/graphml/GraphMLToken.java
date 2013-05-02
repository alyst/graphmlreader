package org.cytoscape.data.reader.graphml;

public enum GraphMLToken {

	// GraphML Tags
	GRAPH("graph"), NODE("node"), EDGE("edge"),
	// GraphML Attributes
	ID("id"), EDGEDEFAULT("edgedefault"), DIRECTED("directed"),
	UNDIRECTED("undirected"), KEY("key"), FOR("for"), ATTRNAME("attr.name"),
	ATTRTYPE("attr.type"), DEFAULT("default"),
	SOURCE("source"), TARGET("target"), DATA("data"), TYPE("type");
	
	private final String tag;
	
	private GraphMLToken(final String tag) {
		this.tag = tag;
	}
	
	public String getTag() {
		return this.tag;
	}
	
	public static GraphMLToken fromString(final String tag) {
		for (GraphMLToken token : GraphMLToken.values()) {
			if(token.getTag().equals(tag))
				return token;
		}
		return null;
	}
}
