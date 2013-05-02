package org.cytoscape.data.reader.graphml;

public class AttributeId {
	final GraphMLScope	scope;
	final String			key;

	public AttributeId( GraphMLScope scope, String key )
	{
		this.scope = scope;
		this.key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		AttributeId other = (AttributeId) obj;
		if (scope != other.scope)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
}
