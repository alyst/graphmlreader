package org.cytoscape.data.reader.graphml;

public enum GraphMLScope {
	NODE("node"), EDGE("edge"),
	GRAPH("graph"), GRAPHML("graphml"),
	ALL("all");

	String tag;

	private GraphMLScope( String tag )
	{
		this.tag = tag;  
	}

	public static GraphMLScope fromString( String tag )
	{
		if ( NODE.tag.equals( tag ) ) return NODE;
		else if ( EDGE.tag.equals( tag ) ) return EDGE;
		else if ( GRAPH.tag.equals( tag ) ) return GRAPH;
		else if ( ALL.tag.equals( tag ) ) return ALL;
		else return null;
	}
}
