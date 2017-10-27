package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.exception.general.InvalidAttribute;
import cz.cas.lib.arclib.exception.general.MissingObject;
import cz.cas.lib.arclib.store.SipProfileStore;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.Utils.*;

@Service
@Slf4j
public class ArclibXmlGenerator {
    private SipProfileStore store;

    private static final String ROOT = "mets";

    private static final String SIMPLE_MAPPING_ELEMENTS_XPATH = "/profile/simpleMapping";
    private static final String AGGREGATION_MAPPING_ELEMENTS_X_PATH = "/profile/aggregationMapping";

    private static final String COUNT = "#@!count!@#";

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
    public String generateArclibXml(String sipPath, String sipProfileId)
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException {
        log.info("Generating ARCLib XML for SIP at path " + sipPath + " using SIP profile with ID " + sipProfileId + ".");

        SipProfile sipProfile = store.find(sipProfileId);
        notNull(sipProfile, () -> new MissingObject(SipProfile.class, sipProfileId));

        String sipProfileXml = sipProfile.getXml();
        notNull(sipProfileXml, () -> new InvalidAttribute(sipProfile, "xml", null));

        Document arclibXml = DocumentHelper.createDocument(DocumentHelper.createElement(ROOT));
        List<Pair<String, String>> nodesToCreate = new ArrayList<>();

        NodeList aggregationMappingNodes = XPathUtils.findWithXPath(stringToInputStream(sipProfileXml), AGGREGATION_MAPPING_ELEMENTS_X_PATH);
        for (int i = 0; i < aggregationMappingNodes.getLength(); i++) {
            nodesToCreateByAggregationMapping((Element) aggregationMappingNodes.item(i), sipPath).forEach(nodesToCreate::add);
        }

        NodeList simpleMappingNodes = XPathUtils.findWithXPath(stringToInputStream(sipProfileXml), SIMPLE_MAPPING_ELEMENTS_XPATH);
        for (int i = 0; i < simpleMappingNodes.getLength(); i++) {
            nodesToCreateBySimpleMapping((Element) simpleMappingNodes.item(i), sipPath).forEach(nodesToCreate::add);
        }

        nodesToCreate.forEach(pair -> {
            String xPath = pair.getL();
            String value = pair.getR();

            XmlBuilder.addNode(arclibXml, xPath, value);
        });

        return arclibXml.asXML().replace("&lt;", "<").replace("&gt;", ">");
    }

    /**
     * Compute nodes to be created in ARCLib XML defined by a simple mapping.
     * 1. finds files in SIP matching regex from mapping
     * 2. in each file finds nodes matching xPath from mapping
     * 3. returns all these nodes
     *
     * @param mappingElement element with the mapping of nodes from the source SIP to ARCLib XML
     * @param sipPath        path to the SIP package
     * @return list of nodes to be created, a node is represented by a pair of xPath to ARCLib XML where it is to be
     * created and its respective value
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    private List<Pair<String, String>> nodesToCreateBySimpleMapping(Element mappingElement, String sipPath)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Element sourceElement = (Element) mappingElement.getElementsByTagName("source").item(0);
        Element destinationElement = (Element) mappingElement.getElementsByTagName("destination").item(0);

        String sourceRootDirPath = sourceElement.getElementsByTagName("rootDirPath").item(0).getTextContent();
        String sourceFileRegex = sourceElement.getElementsByTagName("fileRegex").item(0).getTextContent();
        String sourceXPath = sourceElement.getElementsByTagName("xPath").item(0).getTextContent();

        String destXPath = destinationElement.getElementsByTagName("xPath").item(0).getTextContent();

        List<Pair<String, String>> destinationXPathsToValues = new ArrayList<>();

        File[] files = listFilesMatchingRegex(new File(sipPath + sourceRootDirPath), sourceFileRegex);
        for (int i = 0; i < files.length; i++) {
            NodeList valueNodes = XPathUtils.findWithXPath(new FileInputStream(files[i]), sourceXPath);

            for (int j = 0; j < valueNodes.getLength(); j++) {
                String nodeValue = nodeToString(valueNodes.item(j));
                destinationXPathsToValues.add(new Pair(destXPath, nodeValue));
            }
        }
        return destinationXPathsToValues;
    }

    /**
     * Compute nodes to be created in ARCLib XML defined by an aggregation mapping.
     * 1. finds files in SIP matching regex from mapping
     * 2. in each file finds nodes matching one of the xPaths from mapping and additionally,
     * for each node counts the number of other nodes found with the same value
     * 3. computes cartesian product of the groups of nodes according to the input xPaths under which they were found,
     * result are all the combination that can be created when combining nodes from separate groups,
     * moreover, for every combination it computes how many times this combination appears in the SIP
     * 4. returns list of nodes, one node corresponds one of the combinations from above
     *
     * @param mappingElement element with the mapping of nodes from the source SIP to ARCLib XML
     * @param sipPath        path to the SIP package
     * @return list of nodes to be created, a node is represented by a pair of xPath to ARCLib XML where it is to be
     * created and its respective value
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    private List<Pair<String, String>> nodesToCreateByAggregationMapping(Element mappingElement, String sipPath)
            throws IOException, ParserConfigurationException, SAXException {
        Element sourceElement = (Element) mappingElement.getElementsByTagName("source").item(0);
        Element destinationElement = (Element) mappingElement.getElementsByTagName("destination").item(0);

        String sourceRootDirPath = sourceElement.getElementsByTagName("rootDirPath").item(0).getTextContent();
        String sourceFileRegex = sourceElement.getElementsByTagName("fileRegex").item(0).getTextContent();

        String destXPath = destinationElement.getElementsByTagName("xPath").item(0).getTextContent();

        Element aggregationElement = (Element) destinationElement.getElementsByTagName("aggregation").item(0);
        String aggregationType = aggregationElement.getAttribute("type");
        String aggregationElemName = aggregationElement.getAttribute("destElemName");

        NodeList sourceXPaths = sourceElement.getElementsByTagName("xPath");

        Map<String, String> mapOfNamesToSourceXPaths = new HashMap<>();
        for (int i = 0; i < sourceXPaths.getLength(); i++) {
            String xPathTextContent = sourceXPaths.item(i).getTextContent();
            String xPathAttributeName = sourceXPaths.item(i).getAttributes().getNamedItem("destElemName").getTextContent();
            mapOfNamesToSourceXPaths.put(xPathAttributeName, xPathTextContent);
        }

        File[] files = listFilesMatchingRegex(new File(sipPath + sourceRootDirPath), sourceFileRegex);
        HashMap<String, Map<String, Integer>> namesToValuesAndCount = extractValuesFromFiles(mapOfNamesToSourceXPaths, files);
        List<List<Pair<String, String>>> listOfElementGroups = computeCartesianProduct(namesToValuesAndCount);

        List<Pair<String, String>> destinationXPathsToValues = new ArrayList<>();
        listOfElementGroups.forEach(elementGroup -> {

            String elementGroupValue = elementGroup.stream().map(pair -> {
                String name = pair.getL();
                String value = pair.getR();

                if (name.equals(COUNT)) {
                    if (aggregationType.equals("count")) {
                        return "<" + aggregationElemName + ">" + value + "<" + aggregationElemName + "/>";
                    } else {
                        return "";
                    }
                }

                return "<" + name + ">" + value + "<" + name + "/>";
            }).collect(Collectors.joining());
            destinationXPathsToValues.add(new Pair(destXPath, elementGroupValue));

        });
        return destinationXPathsToValues;
    }

    /**
     * Extract values at the xPaths from the files.
     * 1. for every file and every XPath find the respective nodes
     * 2. for every node compute the number of the nodes found with the same value
     * 3. return map of xPath names as keys and map of entries [node value -> count] as values
     *
     * @param mapOfNamesToSourceXPaths map of xPath names as keys and source xPaths as values
     * @param files                    input files
     * @return result of the extraction,
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private HashMap<String, Map<String, Integer>> extractValuesFromFiles(Map<String, String> mapOfNamesToSourceXPaths, File[] files)
            throws IOException, SAXException, ParserConfigurationException {
        HashMap<String, Map<String, Integer>> namesToValuesAndCount = new HashMap<>();
        for (int i = 0; i < files.length; i++) {
            for (Map.Entry<String, String> entry : mapOfNamesToSourceXPaths.entrySet()) {
                String name = entry.getKey();
                String xPath = entry.getValue();

                NodeList values = XPathUtils.findWithXPath(new FileInputStream(files[i]), xPath);
                for (int j = 0; j < values.getLength(); j++) {
                    Element valueElement = (Element) values.item(j);

                    Map<String, Integer> valuesToCounts = namesToValuesAndCount.get(name);
                    if (valuesToCounts == null) valuesToCounts = new HashMap<>();

                    Integer countOfValues = valuesToCounts.get(valueElement.getTextContent());
                    if (countOfValues == null) countOfValues = 0;

                    valuesToCounts.put(valueElement.getTextContent(), ++countOfValues);
                    namesToValuesAndCount.put(name, valuesToCounts);
                }
            }
        }
        return namesToValuesAndCount;
    }

    /**
     * Computes cartesian product of the groups of nodes according to the input xPaths under which they were found.
     * <p>
     * E.g.:
     * <p>
     * Input:
     * [some/xpath: [A: 1, B: 2]],
     * [another/xpath: [a: 3, b: 4]]
     * <p>
     * Output:
     * [some/xpath: A, another/xpath: a, count: 3],
     * [some/xpath: A, another/xpath: b, count: 4],
     * [some/xpath: B, another/xpath: a, count: 6],
     * [some/xpath: B, another/xpath: b, count: 8]
     *
     * @param map map of xPath names as keys and map of entries [node value -> count] as values
     * @return are all the combination that can be created when combining nodes from separate groups,
     * moreover, for every combination it computes how many times this combination appears
     */
    private List<List<Pair<String, String>>> computeCartesianProduct(Map<String, Map<String, Integer>> map) {
        int solutions = 1;
        for (int i = 0; i < map.keySet().size(); solutions *= map.get(map.keySet().toArray()[i]).size(), i++) ;
        List<List<Pair<String, String>>> result = new ArrayList<>();
        for (int i = 0; i < solutions; i++) {
            List<Pair<String, String>> list = new ArrayList<>();
            int j = 1;
            int count = 1;
            for (Map.Entry<String, Map<String, Integer>> mapEntry : map.entrySet()) {
                Map<String, Integer> value = mapEntry.getValue();
                String key = mapEntry.getKey();

                String subMapKey = (String) value.keySet().toArray()[(i / j) % value.size()];
                list.add(new Pair(key, subMapKey));
                j *= value.size();
                count *= value.get(subMapKey);
            }
            list.add(new Pair(COUNT, String.valueOf(count)));
            result.add(list);
        }
        return result;
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }
}
