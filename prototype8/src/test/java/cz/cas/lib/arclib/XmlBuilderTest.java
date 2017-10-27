package cz.cas.lib.arclib;

import cz.cas.lib.arclib.exception.general.BadArgument;
import cz.cas.lib.arclib.service.XmlBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XmlBuilderTest {
    @Test
    public void addChildNodeTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        XmlBuilder.addNode(doc, "/root/child", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>test value</child></root>"));
    }

    @Test
    public void addGrandChildNodeTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        XmlBuilder.addNode(doc, "/root/child/grandchild", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child><grandchild>test value</grandchild></child></root>"));
    }

    @Test
    public void addSiblingNodeTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        Element root = doc.getRootElement();

        Element child = DocumentHelper.createElement("child");
        child.setText("test value 1");

        root.add(child);

        XmlBuilder.addNode(doc, "/root/child", "test value 2");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>test value 1</child><child>test value 2</child></root>"));
    }

    @Test
    public void addAttributeTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        XmlBuilder.addNode(doc, "/root/@testAttribute", "test value");
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root testAttribute=\"test value\"/>"));
    }

    @Test
    public void addEmptyValueTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        XmlBuilder.addNode(doc, "/root/child", null);
        assertThat(doc.asXML(), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child/></root>"));
    }

    @Test
    public void addToInvalidPathTest() {
        Document doc = DocumentHelper.createDocument(DocumentHelper.createElement("root"));

        assertThrown(() -> XmlBuilder.addNode(doc, "/invalidRoot", "test value")).isInstanceOf(BadArgument.class);
    }
}
