package cz.cas.lib.arclib.solr;

import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.index.Filter;
import cz.cas.lib.arclib.index.IndexFieldConfig;
import cz.cas.lib.arclib.index.IndexStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
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
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SolrStore implements IndexStore {

    private SolrTemplate solrTemplate;

    @Inject private ArclibXmlRepository arclibXmlRepository;

    @SneakyThrows
    public void createIndex(String sipId, int xmlVersion, String arclibXml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xml;
        try {
            xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(arclibXml.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error during parsing of XML document");
            throw e;
        }
        XPath xpath = getXpathWithNamespaceContext();

        ArclibXmlDocument solrArclibXmlDocument = new ArclibXmlDocument();
        Map<String, Object> attributes = new HashMap<>();

        SolrInputDocument doc = new SolrInputDocument();
        for (IndexFieldConfig conf : getFieldsConfig()) {
            NodeList fields = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);
            for (int i = 0; i < fields.getLength(); i++) {
                if (conf.isFullText()) {
                    attributes.put(conf.getFieldName(), nodeToString(fields.item(i)));
                } else {
                    attributes.put(conf.getFieldName(), fields.item(i).getTextContent());
                }
            }
        }
        solrArclibXmlDocument.setId(sipId + "_" + xmlVersion);
        solrArclibXmlDocument.setDocument(arclibXml);
        solrArclibXmlDocument.setAttributes(attributes);
        arclibXmlRepository.save(solrArclibXmlDocument);
    }

    public List<String> findAll(List<Filter> filter) {
        SimpleQuery query = new SimpleQuery(SolrQueryBuilder.nopQuery());
        query.addProjectionOnField("id");
        if (filter.size() > 0)
            query.addFilterQuery(new SimpleFilterQuery(SolrQueryBuilder.buildFilters(filter)));
        log.info("searching for documents");
        Page<ArclibXmlDocument> page;
        try{
            page = solrTemplate.query(query, ArclibXmlDocument.class);
        }catch(UncategorizedSolrException ex){
            if(ex.getMessage() != null && ex.getMessage().contains("undefined field"))
                throw new BadArgument("query contains undefined field");
            throw ex;
        }
        List<String> ids = page.getContent().stream().map(ArclibXmlDocument::getId).collect(Collectors.toList());
        ids.stream().forEach(id -> log.info("found doc with id: " + id));
        return ids;
    }

    private XPath getXpathWithNamespaceContext() {
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

    private String nodeToString(Node node)
            throws TransformerException {
        StringWriter buf = new StringWriter();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(node), new StreamResult(buf));
        return (buf.toString());
    }

    @Inject
    public void setSolrTemplate(SolrTemplate solrTemplate) {
        this.solrTemplate = solrTemplate;
    }
}
