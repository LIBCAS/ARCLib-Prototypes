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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
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

        SolrInputDocument doc = new SolrInputDocument();
        for (IndexFieldConfig conf : getFieldsConfig()) {
            NodeList fields = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);
            for (int i = 0; i < fields.getLength(); i++) {
                switch (conf.getFieldType()) {
                    case DATETIME:
                        Calendar parsedDate = DatatypeConverter.parseDateTime(fields.item(i).getTextContent());
                        String parsedDateTimeString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(parsedDate.toInstant().atZone(ZoneId.systemDefault())) + "Z";
                        solrArclibXmlDocument.addAttribute(conf.getFieldName(), parsedDateTimeString);
                        break;
                    case DATE:
                        parsedDate = DatatypeConverter.parseDate(fields.item(i).getTextContent());
                        String parsedDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(parsedDate.toInstant().atZone(ZoneId.systemDefault())) + "Z";
                        solrArclibXmlDocument.addAttribute(conf.getFieldName(), parsedDateString);
                        break;
                    case TIME:
                        throw new UnsupportedOperationException();
                    default:
                        if (conf.isFullText()) {
                            solrArclibXmlDocument.addAttribute(conf.getFieldName(), nodeToString(fields.item(i)));
                        } else {
                            solrArclibXmlDocument.addAttribute(conf.getFieldName(), fields.item(i).getTextContent());
                        }
                }
            }
        }
        solrArclibXmlDocument.setId(sipId + "_" + xmlVersion);
        solrArclibXmlDocument.setDocument(arclibXml);
        try {
            arclibXmlRepository.save(solrArclibXmlDocument);
        } catch (UncategorizedSolrException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    public List<String> findAll(List<Filter> filter) {
        SimpleQuery query = new SimpleQuery(SolrQueryBuilder.nopQuery());
        query.addProjectionOnField("id");
        if (filter.size() > 0)
            query.addFilterQuery(new SimpleFilterQuery(SolrQueryBuilder.buildFilters(filter)));
        log.info("searching for documents");
        Page<ArclibXmlDocument> page;
        try {
            page = solrTemplate.query(query, ArclibXmlDocument.class);
        } catch (UncategorizedSolrException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("undefined field"))
                throw new BadArgument("query contains undefined field");
            throw ex;
        }
        List<String> ids = page.getContent().stream().map(ArclibXmlDocument::getId).collect(Collectors.toList());
        ids.stream().forEach(id -> log.info("found doc with id: " + id));
        return ids;
    }

    @Inject
    public void setSolrTemplate(SolrTemplate solrTemplate) {
        this.solrTemplate = solrTemplate;
    }
}
