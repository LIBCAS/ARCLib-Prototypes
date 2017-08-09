package cas.lib.arclib.service;

import cas.lib.arclib.ValidationChecker;
import cas.lib.arclib.domain.ValidationProfile;
import cas.lib.arclib.exception.MissingObject;
import cas.lib.arclib.store.ValidationProfileStore;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cas.lib.arclib.util.Utils.notNull;

@Service
public class ValidationService {
    private ValidationProfileStore validationProfileStore;
    private ValidationChecker validationChecker;

    public boolean validateSip(String sipPath, String validationProfileId)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        ValidationProfile validationProfile = validationProfileStore.find(validationProfileId);
        notNull(validationProfile, () -> new MissingObject(ValidationProfile.class, validationProfileId));

        String validationProfileXml = validationProfile.getXml();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document validationProfileDoc = dBuilder.parse(new ByteArrayInputStream(validationProfileXml.getBytes()));
        validationProfileDoc.getDocumentElement().normalize();

        return fileExistenceCheck(sipPath, validationProfileDoc) && validationSchemeCheck(sipPath, validationProfileDoc) &&
                attributeCheck(sipPath, validationProfileDoc);
    }

    private boolean fileExistenceCheck(String sipPath, Document validationProfile) throws XPathExpressionException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList fileExistenceCheckNodes = (NodeList) xPath.compile("/profile/rule/fileExistenceCheck")
                .evaluate(validationProfile, XPathConstants.NODESET);
        for (int i = 0; i< fileExistenceCheckNodes.getLength(); i++) {

            Element element = (Element) fileExistenceCheckNodes.item(i);
            String filePath = element.getElementsByTagName("filePath").item(0).getTextContent();

            if (!validationChecker.fileExistenceCheck(sipPath + filePath)) {
                return false;
            }
        }
        return true;
    }

    private boolean validationSchemeCheck(String sipPath, Document validationProfile) throws XPathExpressionException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList validationSchemeCheckNodes = (NodeList) xPath.compile("/profile/rule/validationSchemeCheck")
                .evaluate(validationProfile,
                        XPathConstants.NODESET);
        for (int i = 0; i< validationSchemeCheckNodes.getLength(); i++) {
            Element validationSchemeCheckElement = (Element) validationSchemeCheckNodes.item(i);
            String filePath = validationSchemeCheckElement.getElementsByTagName("filePath").item(0).getTextContent();
            String scheme = validationSchemeCheckElement.getElementsByTagName("scheme").item(0).getTextContent();

            if(!ValidationChecker.validateWithXMLSchema(sipPath + filePath, scheme)) {
                return false;
            }
        }
        return true;
    }

    private boolean attributeCheck(String sipPath, Document doc)
            throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList attributeCheckNodes = (NodeList) xPath.compile("/profile/rule/attributeCheck")
                .evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i< attributeCheckNodes.getLength(); i++) {
            Element attributeCheckElement = (Element) attributeCheckNodes.item(i);
            String filePath = attributeCheckElement.getElementsByTagName("filePath").item(0).getTextContent();
            String expression = attributeCheckElement.getElementsByTagName("xPath").item(0).getTextContent();
            Path path = Paths.get((sipPath + filePath).substring(1));
            String xml = new String(Files.readAllBytes(path));

            String content =  ValidationChecker.findWithXPath(sipPath + filePath, expression).item(0).getTextContent();

            // compare with value
            Node valueElement = attributeCheckElement.getElementsByTagName("value").item(0);
            if (valueElement != null) {
                if (!valueElement.getTextContent().equals(content)) {
                    return false;
                }
            }
            //compare with regex
            Node regexElement = attributeCheckElement.getElementsByTagName("regex").item(0);
            if (regexElement != null) {
                Pattern p = Pattern.compile(regexElement.getTextContent());
                Matcher m = p.matcher(content);
                if (!m.matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Inject
    public void setValidationProfileStore(ValidationProfileStore validationProfileStore) {
        this.validationProfileStore = validationProfileStore;
    }

    @Inject
    public void setValidationChecker(ValidationChecker validationChecker) {
        this.validationChecker = validationChecker;
    }
}
