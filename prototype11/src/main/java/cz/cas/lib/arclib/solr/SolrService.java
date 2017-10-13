package cz.cas.lib.arclib.solr;

import cz.cas.lib.arclib.config.IndexFieldConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

@Service
@Slf4j
public class SolrService {
    private SolrClient solrClient;

    public void createIndex(String sipId, int xmlVersion, String arclibXml, Set<IndexFieldConfig> indexFieldConfigs) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, SolrServerException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xml;
        try {
            xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(arclibXml.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error during parsing XML document");
            throw e;
        }
        XPath xpath = getXpathWithNamespaceContext();
        SolrInputDocument doc = new SolrInputDocument();
        for (IndexFieldConfig conf : indexFieldConfigs) {
            NodeList fields = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);
            for (int i = 0; i < fields.getLength(); i++) {
                if (conf.isFullText()) {
                    doc.addField(conf.getFieldName(),nodeToString(fields.item(i)));
                } else
                    doc.addField(conf.getFieldName(), fields.item(i).getTextContent());
            }
        }
        doc.addField("sip_id", sipId);
        doc.addField("version", xmlVersion);
        doc.addField("document", arclibXml);
        solrClient.add(doc);
        solrClient.commit();
    }

    private static XPath getXpathWithNamespaceContext() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("No prefix provided!");
                } else if (prefix.equals("METS")) {
                    return "http://www.loc.gov/METS/";
                } else if (prefix.equals("oai_dc")) {
                    return "dc";
                } else if (prefix.equals("premis")) {
                    return "http://www.loc.gov/premis/v3";
                } else if (prefix.equals("ARCLib")) {
                    return "https://www.google.cz";
                } else {
                    return XMLConstants.NULL_NS_URI;
                }
            }

            public String getPrefix(String namespaceURI) {
                // Not needed in this context.
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                // Not needed in this context.
                return null;
            }
        });
        return xpath;
    }

    private static String nodeToString(Node node)
            throws TransformerException {
        StringWriter buf = new StringWriter();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(node), new StreamResult(buf));
        return (buf.toString());
    }

    @Inject
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }
}
