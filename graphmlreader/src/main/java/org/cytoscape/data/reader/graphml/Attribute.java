package org.cytoscape.data.reader.graphml;

import cytoscape.data.CyAttributes;

public class Attribute {
	final AttributeId	id;

	final String			name;
	final GraphMLDataType	datatype;

	public Attribute( GraphMLScope scope, String key, String name, GraphMLDataType datatype )
	{
		this.id = new AttributeId( scope, key );
		this.name = name;
		this.datatype = datatype;
	}

	public void setCyAttribute( CyAttributes attrs, String id, String data )
	{
		//System.err.format( "%s-%s[%s]=%s\n", this.id.scope.tag, id, this.id.key, data );
		switch ( datatype ) {
		case STRING:
			attrs.setAttribute( id, name, data );
			break;
		case BOOLEAN:
			attrs.setAttribute( id, name, Boolean.parseBoolean( data ) );
			break;
		case DOUBLE:
		case FLOAT:
			attrs.setAttribute( id, name, Double.parseDouble( data ) );
			break;
		case INT:
		case LONG:
			attrs.setAttribute( id, name, Integer.parseInt( data ) );
			break;
		default:
			attrs.setAttribute( id, name, data );
		}
	}
}
