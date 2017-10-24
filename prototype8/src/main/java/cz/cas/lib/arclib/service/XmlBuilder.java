package cz.cas.lib.arclib.service;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.Node;

import java.util.List;

@Slf4j
public class XmlBuilder {
    /**
     * Recursive method to create a node and, if necessary, its parents and siblings
     *
     * @param doc         document
     * @param targetXPath to single node
     * @param value       if null an empty node will be created
     * @return the created Node
     */

    public static Node addNode(org.dom4j.Document doc, String targetXPath, String value) {
        log.info("adding Node: " + targetXPath + " -> " + value);

        String elementName = XPathUtils.getChildElementName(targetXPath);
        String parentXPath = XPathUtils.getParentXPath(targetXPath);

        //add value as text to the root element and return
        if (parentXPath == "/") {
            org.dom4j.Element rootElement = doc.getRootElement();
            if (value != null) {
                rootElement.addText(value);
            }
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
}
