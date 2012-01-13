package org.cytoscape.data.writer.graphml;

import giny.view.EdgeView;
import giny.view.NodeView;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;

public class GraphMLWriter {
	
	private static final String GRAPHMLNS_URL = "http://graphml.graphdrawing.org/xmlns";
	private static final String YWORKSNS_URL = "http://www.yworks.com/xml/graphml";
	private static final String GRAPHML = "graphml";
	private static final String GRAPH = "graph";
	private static final String ID = "id";
	private static final String NODE = "node";
	private static final String EDGE = "edge";
	private static final String DATA = "data";

	private static final String SOURCE = "source";
	private static final String TARGET = "target";
	
	private static final String directed = "edgedefault";
	
	private static final String GRAPHICS_ATTRID = "__YWorks_Graphics__";

	private static final Map<Integer, String> shapeCodeMap;
	
	static {
		shapeCodeMap = new HashMap<Integer, String>();
		shapeCodeMap.put( NodeView.DIAMOND, "diamond" );
		shapeCodeMap.put( NodeView.ELLIPSE, "ellipse" );
		shapeCodeMap.put( NodeView.HEXAGON, "hexagon" );
		shapeCodeMap.put( NodeView.OCTAGON, "octagon" );
		shapeCodeMap.put( NodeView.PARALELLOGRAM, "parallelogram" );
		shapeCodeMap.put( NodeView.RECTANGLE, "rectangle" );
		shapeCodeMap.put( NodeView.ROUNDED_RECTANGLE, "roundrectangle" );
		shapeCodeMap.put( NodeView.TRIANGLE, "triangle" );
		shapeCodeMap.put( NodeView.VEE, "trapezoid2" );
	}

	private static final Map<Integer, String> arrowCodeMap;

	static {
		arrowCodeMap = new HashMap<Integer, String>();
		arrowCodeMap.put( EdgeView.NO_END, "none" );
		arrowCodeMap.put( EdgeView.BLACK_ARROW, "standard" );
		arrowCodeMap.put( EdgeView.WHITE_ARROW, "white_delta" ); // @TODO: no exact match
		arrowCodeMap.put( EdgeView.BLACK_DELTA, "delta" );
		arrowCodeMap.put( EdgeView.WHITE_DELTA, "white_delta" );
		arrowCodeMap.put( EdgeView.BLACK_DIAMOND, "diamond" );
		arrowCodeMap.put( EdgeView.WHITE_DIAMOND, "white_diamond" );
		arrowCodeMap.put( EdgeView.BLACK_CIRCLE, "circle" );
		arrowCodeMap.put( EdgeView.WHITE_CIRCLE, "transparent_circle" );
		arrowCodeMap.put( EdgeView.BLACK_T, "t_shape" );
		arrowCodeMap.put( EdgeView.WHITE_T, "t_shape" ); // @TODO: no exact match
	}

	private final CyNetwork network;
	private final CyNetworkView networkView;
	private final Writer writer;
	private final TaskMonitor monitor;

	private	DocumentBuilderFactory factory;
	private	DocumentBuilder builder;
	private Document doc;
	private Map<String, String> attrIdMap;

	private static String EncodeCytoscapeAttr( String objectType, String name ) {
		return objectType + ':' + name;
	}

	/**
	 * Get the String representation of the 6 character hexadecimal RGB values
	 * i.e. #ff000a
	 *
	 * @param Color
	 *            The color to be converted
	 */
	private static String ColorHexString(final Color c) {
		return ("#" // +Integer.toHexString(c.getRGB());
		       + Integer.toHexString(256 + c.getRed()).substring(1)
		       + Integer.toHexString(256 + c.getGreen()).substring(1)
		       + Integer.toHexString(256 + c.getBlue()).substring(1));
	}
	
	public GraphMLWriter(final CyNetwork network, final CyNetworkView networkView, final Writer writer,
			final TaskMonitor taskMonitor) {
		this.network = network;
		this.networkView = networkView;
		this.writer = writer;
		this.monitor = taskMonitor;
		factory = null;
		builder = null;
		doc = null;
		attrIdMap = null;
	}

	public void write() throws IOException, ParserConfigurationException, TransformerException {
		initDocument();

		TransformerFactory transFactory = TransformerFactory.newInstance();
		transFactory.setAttribute("indent-number", 4);
		Transformer transformer = transFactory.newTransformer();
		
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		
		DOMSource source = new DOMSource(doc);
		
		StreamResult result = new StreamResult(writer); 
		transformer.transform(source, result);

		doc = null;
	}

	private void initDocument() throws ParserConfigurationException {
		if ( factory == null ) factory = DocumentBuilderFactory.newInstance();
		if ( builder == null ) builder = factory.newDocumentBuilder();
		if ( doc != null ) throw new RuntimeException( "document already initialized" );
		doc = builder.newDocument();
		attrIdMap = new HashMap<String, String>();
		
		Element root = doc.createElementNS(GRAPHMLNS_URL, GRAPHML);
		root.setAttribute( "xmlns", GRAPHMLNS_URL );
		root.setAttribute( "xmlns:y", YWORKSNS_URL );
		root.setAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
		root.setAttribute( "xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd" );
		root.appendChild( doc.createComment( "Generated by GraphMLReader Cytoscape plugin") ); // @TODO: write plugin's version
		doc.appendChild(root);

		// write cytoscape attributes
		writeAttributes(Cytoscape.getNetworkAttributes(), GRAPH, root);

		writeAttributes(Cytoscape.getNodeAttributes(), NODE, root);
		// YFiles node graphics attributes
		if ( networkView != null ) {
			String graphmlId = NODE.substring(0, 1) + ( attrIdMap.size() + 1 );
			attrIdMap.put( EncodeCytoscapeAttr(NODE, GRAPHICS_ATTRID), graphmlId );
			Element keyElm = doc.createElement("key");
			keyElm.setAttribute("for", NODE );
			keyElm.setAttribute(ID, graphmlId);
			keyElm.setAttribute("yfiles.type", "nodegraphics");
			root.appendChild( keyElm );
		}

		writeAttributes(Cytoscape.getEdgeAttributes(), EDGE, root);
		// YFiles edge graphics attributes
		if ( networkView != null ) {
			String graphmlId = EDGE.substring(0, 1) + ( attrIdMap.size() + 1 );
			attrIdMap.put( EncodeCytoscapeAttr(EDGE, GRAPHICS_ATTRID), graphmlId );
			Element keyElm = doc.createElement("key");
			keyElm.setAttribute("for", EDGE );
			keyElm.setAttribute(ID, graphmlId);
			keyElm.setAttribute("yfiles.type", "edgegraphics");
			root.appendChild( keyElm );
		}

		// write the network
		Element graphElm = doc.createElement(GRAPH);
		// For now, everything is directed.
		graphElm.setAttribute(directed, "directed");
		graphElm.setAttribute(ID, network.getTitle());
		appendData( GRAPH, Cytoscape.getNetworkAttributes(), graphElm, network.getIdentifier() );
		root.appendChild(graphElm);
		
		writeNodes(graphElm);
		writeEdges(graphElm);
	}

	private Element attributeDefinitionNode( String objectType, String name, String type )
	{
		Element keyElm = doc.createElement("key");
		keyElm.setAttribute("for", objectType );
		keyElm.setAttribute("attr.name", name );
		keyElm.setAttribute("attr.type", type );
		String graphmlId = objectType.substring(0, 1) + ( attrIdMap.size() + 1 );
		keyElm.setAttribute(ID, graphmlId );
		attrIdMap.put( EncodeCytoscapeAttr(objectType, name), graphmlId );
		return keyElm;
	}

	private void writeAttributes(CyAttributes attrs, String objectType, Element parent) {
		final String[] nodeAttrNames = attrs.getAttributeNames();
		for(String attrName : nodeAttrNames) {
			final Class<?> type = CyAttributesUtils.getClass(attrName, attrs);
			String tag = GraphMLAttributeDataTypes.getTag(type);
			if(tag == null)
				tag = GraphMLAttributeDataTypes.STRING.getTypeTag();
			parent.appendChild( attributeDefinitionNode( objectType, attrName, tag ) );
		}
	}
	
	private void writeNodes(Element parent) {
		final List<CyNode> nodes = network.nodesList();
		
		for(final CyNode node: nodes) {
			final Element nodeElm = doc.createElement(NODE);
			nodeElm.setAttribute(ID, node.getIdentifier());
			appendData(NODE, Cytoscape.getNodeAttributes(), nodeElm, node.getIdentifier());

			if ( networkView != null ) {
				final NodeView nodeView = networkView.getNodeView(node);
				final Element shapeNode = doc.createElement("y:ShapeNode");

				final Element geometry = doc.createElement("y:Geometry");
				geometry.setAttribute("height", String.valueOf( nodeView.getHeight() ) );
				geometry.setAttribute("width", String.valueOf( nodeView.getWidth() ) );
				geometry.setAttribute("x", String.valueOf( nodeView.getXPosition() - 0.5 * nodeView.getWidth() ) );
				geometry.setAttribute("y", String.valueOf( nodeView.getYPosition() - 0.5 * nodeView.getHeight() ) );
				shapeNode.appendChild( geometry );

				final Element fill = doc.createElement("y:Fill");
				fill.setAttribute("color", ColorHexString((Color)nodeView.getUnselectedPaint()) );
				fill.setAttribute("transparent", String.valueOf( nodeView.getUnselectedPaint().getTransparency() != Paint.OPAQUE ) );
				shapeNode.appendChild( fill );

				final Element borderStyle = doc.createElement("y:BorderStyle");
				borderStyle.setAttribute("color", ColorHexString((Color)nodeView.getBorderPaint()) );
				borderStyle.setAttribute("type", "line" ); // @TODO use getBorder()
				borderStyle.setAttribute("width", String.valueOf( nodeView.getBorderWidth() ) );
				shapeNode.appendChild( borderStyle );

				final Element nodeLabel = doc.createElement("y:NodeLabel");
				nodeLabel.setTextContent( nodeView.getLabel().getText() );
				//nodeLabel.setAttribute("alignment", "center" ); // @TODO
				//nodeLabel.setAttribute("autoSizePolicy", "content" ); // @TODO
				//nodeLabel.setAttribute("borderDistance", "0.0" ); // @TODO
				nodeLabel.setAttribute("fontFamily", nodeView.getLabel().getFont().getFamily() );
				nodeLabel.setAttribute("fontSize", String.valueOf( nodeView.getLabel().getFont().getSize() ) );
				//nodeLabel.setAttribute("fontStyle", "plain" ); // @TODO
				//nodeLabel.setAttribute("hasBackgroundColor", "false" ); // @TODO
				//nodeLabel.setAttribute("hasLineColor", "false" ); // @TODO
				nodeLabel.setAttribute("hasText", String.valueOf( nodeView.getLabel().getText() != null ) );
				//nodeLabel.setAttribute("height", "4.0" ); // @TODO
				//nodeLabel.setAttribute("modelName", "internal" ); // @TODO
				//nodeLabel.setAttribute("modelPosition", "c" ); // @TODO
				nodeLabel.setAttribute("textColor", ColorHexString((Color)nodeView.getLabel().getTextPaint()) );
				//nodeLabel.setAttribute("visible", String.valueOf( nodeView.getLabel().getText() != null ) ); // @TODO
				//nodeLabel.setAttribute("width", String.valueOf( nodeView.getLabelWidth() ) );
				//nodeLabel.setAttribute("x", String.valueOf( nodeView.getLabel().getPosition().getOffsetX() ) );
				//nodeLabel.setAttribute("y", String.valueOf( nodeView.getLabel().getPosition().getOffsetY() ) );
				shapeNode.appendChild( nodeLabel );

				final Element shape = doc.createElement("y:Shape");
				shape.setAttribute("type", shapeCodeMap.get( nodeView.getShape() ) );
				shapeNode.appendChild( shape );

				appendDataAttr(NODE, nodeElm, GRAPHICS_ATTRID, shapeNode);
			}
			parent.appendChild(nodeElm);
		}
	}
	
	private void writeEdges(Element parent) {
		final List<CyEdge> edges = network.edgesList();
		
		for(final CyEdge edge: edges) {
			final Element edgeElm = doc.createElement(EDGE);
			edgeElm.setAttribute(SOURCE, edge.getSource().getIdentifier());
			edgeElm.setAttribute(TARGET, edge.getTarget().getIdentifier());
			appendData(EDGE, Cytoscape.getEdgeAttributes(), edgeElm, edge.getIdentifier());
			if ( networkView != null ) {
				final EdgeView edgeView = networkView.getEdgeView(edge);
				final Element curveNode = doc.createElement( edgeView.getLineType() == EdgeView.CURVED_LINES
						                                     ? "y:SplineEdge" : "y:PolyLine" );

				final Element path = doc.createElement("y:Path");
				path.setAttribute("sx", "0.0" );
				path.setAttribute("sy", "0.0" );
				path.setAttribute("tx", "0.0" );
				path.setAttribute("ty", "0.0" );
				curveNode.appendChild( path );

				final Element lineStyle = doc.createElement("y:LineStyle");
				lineStyle.setAttribute("color", ColorHexString((Color)edgeView.getUnselectedPaint()) );
				lineStyle.setAttribute("type", "line" ); // @TODO use getStroke()
				lineStyle.setAttribute("width", String.valueOf( edgeView.getStrokeWidth() ) );
				curveNode.appendChild( lineStyle );

				final Element arrows = doc.createElement("y:Arrows");
				arrows.setAttribute("source", arrowCodeMap.containsKey( edgeView.getSourceEdgeEnd() )
						                      ? arrowCodeMap.get( edgeView.getSourceEdgeEnd()) : "none" );
				arrows.setAttribute("target", arrowCodeMap.containsKey( edgeView.getTargetEdgeEnd() )
						                      ? arrowCodeMap.get( edgeView.getSourceEdgeEnd()) : "none" );
				curveNode.appendChild( arrows );

				appendDataAttr(EDGE, edgeElm, GRAPHICS_ATTRID, curveNode);
			}
			parent.appendChild(edgeElm);
		}
	}
	
	private void appendDataAttr( String objectType, Element parent, String attrName, Object val ) {
		if (val != null) {
			final Element dataElm = doc.createElement(DATA);
			dataElm.setAttribute("key", attrIdMap.get( EncodeCytoscapeAttr( objectType, attrName ) ) );
			if ( val instanceof Element ) {
				dataElm.appendChild( (Element)val );
			} else {
				dataElm.setTextContent(val.toString());
			}
			parent.appendChild( dataElm );
		}
	}
	private void appendData(String objectType, CyAttributes attrs, Element parent, String id) {
		final String[] attrNames = attrs.getAttributeNames();
		
		for(String name: attrNames) {
			appendDataAttr(objectType, parent, name, attrs.getAttribute(id, name));
		}
	}

}
