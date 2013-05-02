package org.cytoscape.data.reader.graphml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.text.StrBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;

/**
 * A SAX Parser for GraphML data file.
 * @author Kozo.Nishida
 *
 */
public class GraphMLParser extends DefaultHandler {

	// private static CyLogger logger = CyLogger.getLogger(GraphMLParser.class);

	private String networkName = null;

	/* Internal lists of the created nodes and edges */
	private List<CyNode> nodeList = null;
	private List<CyEdge> edgeList = null;

	/* Map of XML ID's to nodes */
	private Map<String, CyNode> nodeidMap = null;

	/* Map of data type to nodes or edges */

	private Map<AttributeId, Attribute> attributeMap = null;

	private CyNode currentNode = null;
	private CyEdge currentEdge = null;

	// Attribute values
	private GraphMLScope currentScope = null;
	private String currentId;
	private String currentAttributeKey = null;
	private StrBuilder currentAttributeData = null;
	private String currentEdgeSource = null;
	private String currentEdgeTarget = null;

	private Map<GraphMLScope, CyAttributes> cyAttributes;

	/* node, edge, data parsing */
	private boolean directed = false;

	/********************************************************************
	 * Routines to handle keys
	 *******************************************************************/

	/**
	 * Main constructor for our parser. Initialize any local arrays. Note that
	 * this parser is designed to be as memory efficient as possible. As a
	 * result, a minimum number of local data structures
	 */
	GraphMLParser() {
		nodeList = new ArrayList<CyNode>();
		edgeList = new ArrayList<CyEdge>();
		nodeidMap = new HashMap<String, CyNode>();
		currentAttributeData = new StrBuilder();

		attributeMap = new HashMap<AttributeId, Attribute>();
		currentScope = GraphMLScope.GRAPHML;
		cyAttributes = new HashMap<GraphMLScope, CyAttributes>();
		cyAttributes.put(GraphMLScope.NODE, Cytoscape.getNodeAttributes() );
		cyAttributes.put(GraphMLScope.EDGE, Cytoscape.getEdgeAttributes() );
		cyAttributes.put(GraphMLScope.GRAPH, Cytoscape.getNetworkAttributes() );
	}

	/********************************************************************
	 * Interface routines. These routines are called by the GraphMLReader to get
	 * the resulting data.
	 *******************************************************************/

	int[] getNodeIndicesArray() {

		System.out.println("Got nodes: " + nodeList.size());

		int[] array = new int[nodeList.size()];

		for (int i = 0; i < nodeList.size(); i++) {
			array[i] = nodeList.get(i).getRootGraphIndex();
		}
		return array;
	}

	int[] getEdgeIndicesArray() {

		System.out.println("Got edges: " + edgeList.size());

		int[] array = new int[edgeList.size()];
		for (int i = 0; i < edgeList.size(); i++) {
			array[i] = edgeList.get(i).getRootGraphIndex();
		}
		return array;
	}

	String getNetworkName() {
		return networkName;
	}

	/********************************************************************
	 * Handler routines. The following routines are called directly from the SAX
	 * parser.
	 *******************************************************************/

	@Override
	public void startDocument() {

	}

	@Override
	public void endDocument() throws SAXException {

	}

	@Override
	public void startElement(String namespace, String localName, String qName,
			Attributes atts) throws SAXException {
		if (qName.equals(GraphMLToken.GRAPH.getTag())) {
			currentScope = GraphMLScope.GRAPH;
			currentId = Cytoscape.getCurrentNetwork().getIdentifier();
			// parse directed or undirected
			String edef = atts.getValue(GraphMLToken.EDGEDEFAULT.getTag());
			directed = GraphMLToken.DIRECTED.getTag().equalsIgnoreCase(edef);

			this.networkName = atts.getValue(GraphMLToken.ID.getTag());

		} else if (qName.equals(GraphMLToken.KEY.getTag())) {
			GraphMLScope scope = GraphMLScope.fromString( atts.getValue(GraphMLToken.FOR.getTag() ) );
			Attribute attr = new Attribute( scope,
					atts.getValue(GraphMLToken.ID.getTag() ),
					atts.getValue(GraphMLToken.ATTRNAME.getTag()),
					GraphMLDataType.fromString( atts.getValue(GraphMLToken.ATTRTYPE.getTag()) ) );
			if ( scope == GraphMLScope.ALL ) {
				// if defined in every scope
				for ( final GraphMLScope eachScope : GraphMLScope.values() ) {
					if ( eachScope != GraphMLScope.ALL ) {
						Attribute eachAttr = new Attribute( eachScope, attr.id.key,
								attr.name, attr.datatype );
						attributeMap.put( eachAttr.id, eachAttr );
					}
				}
			} else {
				attributeMap.put( attr.id,  attr );
			}
		} else if (qName.equals(GraphMLToken.NODE.getTag())) {
			currentScope = GraphMLScope.NODE;
			// Parse node entry.
			currentId = atts.getValue(GraphMLToken.ID.getTag());
			currentNode = Cytoscape.getCyNode(currentId, true);
			nodeList.add(currentNode);
			nodeidMap.put(currentId, currentNode);
		} else if (qName.equals(GraphMLToken.EDGE.getTag())) {
			currentScope = GraphMLScope.EDGE;
			// Parse edge entry
			currentEdgeSource = atts.getValue(GraphMLToken.SOURCE.getTag());
			currentEdgeTarget = atts.getValue(GraphMLToken.TARGET.getTag());
			CyNode sourceNode = nodeidMap.get(currentEdgeSource);
			CyNode targetNode = nodeidMap.get(currentEdgeTarget);
			currentEdge = Cytoscape.getCyEdge(sourceNode, targetNode,
					Semantics.INTERACTION, "pp", true);
			currentId = currentEdge.getIdentifier();
			edgeList.add(currentEdge);
		} else if (qName.equals(GraphMLToken.DATA.getTag())) {
			currentAttributeData.clear();
			currentAttributeKey = atts.getValue(GraphMLToken.KEY.getTag());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		currentAttributeData.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		if (qName == GraphMLToken.DATA.getTag()) {
			String dataText = currentAttributeData.toString().trim();
			if ( dataText != null && dataText.length() > 0 ) {
				final Attribute attr = attributeMap.get( new AttributeId( currentScope, currentAttributeKey ) );
				if ( attr != null ) {
					attr.setCyAttribute( cyAttributes.get( currentScope ), currentId, dataText );
				} // FIXME warning attribute not found
			}
			currentAttributeData.clear();
		}
		else if ( qName == GraphMLToken.NODE.getTag()
				|| qName == GraphMLToken.EDGE.getTag()
		){
			currentScope = GraphMLScope.GRAPH; // FIXME what if nested?
			currentId = Cytoscape.getCurrentNetwork().getIdentifier();
			currentNode = null;
			currentEdge = null;
		}
	}

}
