package org.cytoscape.data.writer.graphml;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

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

public class GraphMLWriter {
	
	private static final String GRAPHMLNS_URL = "http://graphml.graphdrawing.org/xmlns";
	private static final String GRAPHML = "graphml";
	private static final String GRAPH = "graph";
	private static final String ID = "id";
	private static final String NODE = "node";
	private static final String EDGE = "edge";
	
	private static final String SOURCE = "source";
	private static final String TARGET = "target";
	
	private static final String directed = "edgedefault";
	
	
	

	private final CyNetwork network;
	private final Writer writer;
	private final TaskMonitor monitor;

	private	DocumentBuilderFactory factory;
	private	DocumentBuilder builder;
	private Document doc;

	public GraphMLWriter(final CyNetwork network, final Writer writer,
			final TaskMonitor taskMonitor) {
		this.network = network;
		this.writer = writer;
		this.monitor = taskMonitor;
		factory = null;
		builder = null;
		doc = null;
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
		
		Element root = doc.createElementNS(GRAPHMLNS_URL, GRAPHML);
		root.setAttribute( "xmlns", GRAPHMLNS_URL );
		root.setAttribute( "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance" );
		root.setAttribute( "xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns" );
		root.appendChild( doc.createComment( "Generated by GraphMLReader Cytoscape plugin") ); // @TODO: write plugin's version
		doc.appendChild(root);

		Element graphElm = doc.createElement(GRAPH);
		
		// For now, everything is directed.
		graphElm.setAttribute(directed, "directed");
		graphElm.setAttribute(ID, network.getTitle());
		
		root.appendChild(graphElm);
		
		writeAttributes(Cytoscape.getNodeAttributes(), root);
		writeAttributes(Cytoscape.getEdgeAttributes(), root);
		writeAttributes(Cytoscape.getNetworkAttributes(), root);
		
		writeNodes(graphElm);
		writeEdges(graphElm);
	}

	private void writeAttributes(CyAttributes attrs, Element parent) {
		final String[] nodeAttrNames = attrs.getAttributeNames();
		for(String attrName : nodeAttrNames) {
			final Class<?> type = CyAttributesUtils.getClass(attrName, attrs);
			String tag = GraphMLAttributeDataTypes.getTag(type);
			if(tag == null)
				tag = GraphMLAttributeDataTypes.STRING.getTypeTag();

			Element keyElm = doc.createElement("key");
			keyElm.setAttribute("for", NODE);
			keyElm.setAttribute("attr.name", attrName);
			keyElm.setAttribute("attr.type", tag);
			keyElm.setAttribute(ID, attrName);
			parent.appendChild(keyElm);
		}
	}
	
	private void writeNodes(Element parent) {
		final List<CyNode> nodes = network.nodesList();
		
		for(final CyNode node: nodes) {
			final Element nodeElm = doc.createElement(NODE);
			nodeElm.setAttribute(ID, node.getIdentifier());
			appendData(Cytoscape.getNodeAttributes(), nodeElm, node.getIdentifier());
			parent.appendChild(nodeElm);
		}
	}
	
	private void writeEdges(Element parent) {
		final List<CyEdge> edges = network.edgesList();
		
		for(final CyEdge edge: edges) {
			final Element edgeElm = doc.createElement(EDGE);
			edgeElm.setAttribute(SOURCE, edge.getSource().getIdentifier());
			edgeElm.setAttribute(TARGET, edge.getTarget().getIdentifier());
			appendData(Cytoscape.getEdgeAttributes(), edgeElm, edge.getIdentifier());
			parent.appendChild(edgeElm);
		}
	}
	
	private void appendData(CyAttributes attrs, Element parent, String id) {
		final String[] attrNames = attrs.getAttributeNames();
		
		for(String name: attrNames) {
			Object val = attrs.getAttribute(id, name);
			if(val != null) {
				Element dataElm = doc.createElement("data");
				dataElm.setAttribute("key", name);
				dataElm.setTextContent(val.toString());
				parent.appendChild(dataElm);
			}
			
		}
	}

}
