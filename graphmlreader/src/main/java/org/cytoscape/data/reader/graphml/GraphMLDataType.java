package org.cytoscape.data.reader.graphml;

import java.util.Date;

public enum GraphMLDataType {
	// Supported attribute data types
	INT("int", Integer.class), LONG("long", Long.class), FLOAT("float", Float.class), DOUBLE("double", Double.class),
	BOOLEAN("boolean", Boolean.class), STRING("string", String.class), DATE("date", Date.class);

	private final String tag;
	final Class<?> dataType;
	
	private GraphMLDataType( final String tag, final Class<?> dataType) {
		this.tag = tag;
		this.dataType = dataType;
	}

	public static GraphMLDataType fromString( String tag )
	{
		if ( INT.tag.equals( tag ) ) return INT;
		else if ( LONG.tag.equals( tag ) ) return LONG;
		else if ( FLOAT.tag.equals( tag ) ) return FLOAT;
		else if ( DOUBLE.tag.equals( tag ) ) return DOUBLE;
		else if ( BOOLEAN.tag.equals( tag ) ) return BOOLEAN;
		else if ( STRING.tag.equals( tag ) ) return STRING;
		else if ( DATE.tag.equals( tag ) ) return DATE;
		else return null;
	}

	public String getTag() {
		return this.tag;
	}

	public Class<?> getDataType() {
		return this.dataType;
	}

	public Object convertValue(final String value) { 
		if(dataType == String.class)
			return value;
		else if(dataType == Double.class)
			return Double.parseDouble(value);
		
		return null;
	}
}
