package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.Utils;
import cz.cas.lib.arclib.exception.MissingNode;
import cz.cas.lib.arclib.exception.general.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArclibXmlValidator {

    private Resource arclibXmlValidationChecks;

    /**
     * Validates the structure of ARCLib XML. Validator checks presence of the given nodes in ARCLib XML according to the XPaths specified
     * in the file <i>arclibXmlValidationChecks.txt</i>.
     *
     * @param xml ARCLib XML to validate
     * @throws IOException if file with ARCLib XML validation checks does not exist
     */
    public void validateArclibXml(String xml) throws IOException {
        List<String> xPaths = readLinesOfFileToList(arclibXmlValidationChecks.getFile());

        xPaths.forEach(xPath -> {
            try {
                checkNodeExists(xml, xPath);
            } catch (ParserConfigurationException e) {
                throw new GeneralException(e);
            } catch (IOException e) {
                throw new GeneralException(e);
            } catch (XPathExpressionException e) {
                throw new GeneralException(e);
            } catch (SAXException e) {
                throw new GeneralException(e);
            }
        });
    }

    /**
     * Reads the lines of the file to a {@link List<String>}
     *
     * @param file file to read from
     * @return {@link List<String>} with lines of the file
     * @throws IOException if the file does not exist
     */
    private List<String> readLinesOfFileToList(File file) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Checks if the node at the xPath exists in the XML. If the node does not exist, {@link MissingNode} exception is thrown.
     *
     * @param xml xml
     * @param xPath xPath
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private void checkNodeExists(String xml, String xPath) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        NodeList withXPath = ValidationChecker.findWithXPath(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name())), xPath);

        Utils.ne(withXPath.getLength(), 0, () -> new MissingNode(xPath, xml));
    }

    @Inject
    public void setArclibXmlValidationChecks(@Value("${arclib.arclibXmlValidationChecks}") Resource arclibXmlValidationChecks) {
        this.arclibXmlValidationChecks = arclibXmlValidationChecks;
    }
}
