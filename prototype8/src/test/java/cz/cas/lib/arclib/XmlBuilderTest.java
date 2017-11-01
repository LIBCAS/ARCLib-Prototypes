package cz.cas.lib.arclib;

import cz.cas.lib.arclib.service.XmlBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XmlBuilderTest {

    private XmlBuilder xmlBuilder;

    @Before
    public void setUp() {
        Map<String, String> uris = new HashMap<>();
        uris.put("METS", "http://www.loc.gov/METS/");
        uris.put("ARCLIB", "http://arclib.lib.cas.cz/ARCLIB_XML");
        uris.put("PREMIS", "http://www.loc.gov/premis/v3");

        xmlBuilder = new XmlBuilder(uris);
    }

    @Test
    public void addChildNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>test value</child></root>"));
    }

    @Test
    public void addGrandChildNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child/grandchild", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child><grandchild>test value</grandchild></child></root>"));
    }

    @Test
    public void addSiblingNodeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        Element root = doc.getRootElement();

        Element child = DocumentHelper.createElement("child");
        child.setText("test value 1");

        root.add(child);

        xmlBuilder.addNode(doc, "/root/child", "test value 2");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>test value 1</child><child>test value 2</child></root>"));
    }

    @Test
    public void addAttributeTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/@testAttribute", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root testAttribute=\"test value\"/>"));
    }

    @Test
    public void addEmptyValueTest() throws IOException, SAXException, TransformerException {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        xmlBuilder.addNode(doc, "/root/child", null);
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child/></root>"));
    }
}
