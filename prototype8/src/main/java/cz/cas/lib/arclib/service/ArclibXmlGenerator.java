package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.exception.general.InvalidAttribute;
import cz.cas.lib.arclib.exception.general.MissingObject;
import cz.cas.lib.arclib.store.SipProfileStore;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cz.cas.lib.arclib.Utils.notNull;

@Service
@Slf4j
public class ArclibXmlGenerator {
    private SipProfileStore store;

    private static final String ROOT = "arclibXml";
    private static final String MAPPING_NODES_X_PATH = "/profile/mapping";

    /**
     * Generates ARCLib XML from SIP using the SIP profile
     *
     * @param sipPath      path to the SIP package
     * @param sipProfileId id of the SIP profile
     * @return ARCLib XML
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    public String generateArclibXml(String sipPath, String sipProfileId) throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, TransformerException {
        Document arclibXml = DocumentHelper.createDocument(DocumentHelper.createElement(ROOT));

        SipProfile sipProfile = store.find(sipProfileId);
        notNull(sipProfile, () -> new MissingObject(SipProfile.class, sipProfileId));

        String xml = sipProfile.getXml();
        notNull(xml, () -> new InvalidAttribute(sipProfile, "xml", null));

        NodeList mappingNodes = XPathUtils.findWithXPath(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name())),
                MAPPING_NODES_X_PATH);

        if (mappingNodes != null) {
            for (int i = 0; i < mappingNodes.getLength(); i++) {
                if (mappingNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element mappingElement = (Element) mappingNodes.item(i);
                    addNodesByMapping(arclibXml, mappingElement, sipPath);
                }
            }
        }
        return arclibXml.asXML().replace("&lt;", "<").replace("&gt;", ">");
    }

    /**
     * Adds nodes to ARCLib XML using the provided node mapping
     *
     * @param arclibXml      document with ARCLib XML
     * @param mappingElement element with the mapping of nodes from the source SIP to ARCLib XML
     * @param sipPath        path to the SIP package
     * @throws XPathExpressionException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void addNodesByMapping(Document arclibXml, Element mappingElement, String sipPath)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        Element source = (Element) mappingElement.getElementsByTagName("source").item(0);

        String sourceFilePath = source.getElementsByTagName("filePath").item(0).getTextContent();
        String sourceXPath = source.getElementsByTagName("xPath").item(0).getTextContent();

        Element destination = (Element) mappingElement.getElementsByTagName("destination").item(0);
        String destXPath = destination.getElementsByTagName("xPath").item(0).getTextContent();

        FileInputStream sourceFileStream = new FileInputStream(sipPath + "/" + sourceFilePath);

        NodeList valueNodes = XPathUtils.findWithXPath(sourceFileStream, sourceXPath);

        for (int i = 0; i < valueNodes.getLength(); i++) {
            String nodeValue = nodeToString(valueNodes.item(i));
            addNode(arclibXml, destXPath, nodeValue);
        }
    }

    /**
     * Transforms {@link Node} to {@link String}
     *
     * @param node node to transform
     * @return {@link String} representation of the {@link Node}
     */
    private static String nodeToString(org.w3c.dom.Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    /**
     * Recursive method to create a node and, if necessary, its parents and siblings
     *
     * @param doc         document
     * @param targetXPath to single node
     * @param value       if null an empty node will be created
     * @return the created Node
     */
    private Node addNode(Document doc, String targetXPath, String value) {
        log.info("adding Node: " + targetXPath + " -> " + value);

        String elementName = XPathUtils.getChildElementName(targetXPath);
        String parentXPath = XPathUtils.getParentXPath(targetXPath);

        //add value as text to the root element and return
        if (parentXPath == "/") {
            org.dom4j.Element rootElement = doc.getRootElement();
            rootElement.addText(value);
            return rootElement;
        }

        Node parentNode = doc.selectSingleNode(parentXPath);
        if (parentNode == null) {
            parentNode = addNode(doc, parentXPath, null);
        }

        //add value as attribute to the parent node and return
        if (elementName.startsWith("@")) {
            return ((org.dom4j.Element) parentNode).addAttribute(elementName.substring(1), value);
        }

        // create younger siblings if needed
        Integer childIndex = XPathUtils.getChildElementIndex(targetXPath);
        if (childIndex > 1) {
            List<?> nodelist = doc.selectNodes(XPathUtils.createPositionXpath(targetXPath, childIndex));
            // how many to create = (index wanted - existing - 1 to account for the new element we will create)
            int nodesToCreate = childIndex - nodelist.size() - 1;
            for (int i = 0; i < nodesToCreate; i++) {
                ((org.dom4j.Element) parentNode).addElement(elementName);
            }
        }

        //add new element to the parent node
        org.dom4j.Element created = ((org.dom4j.Element) parentNode).addElement(elementName);
        if (null != value) {
            created.addText(value);
        }

        return created;
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }
}
