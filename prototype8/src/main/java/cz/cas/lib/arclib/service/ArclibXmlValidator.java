package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.Utils;
import cz.cas.lib.arclib.exception.general.BadArgument;
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

    public void validateArclibXml(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException {
        List<String> lines = readFileToList(arclibXmlValidationChecks.getFile());

        lines.forEach(xPath -> {
            try {
                checkNodeExists(xml, xPath);
            } catch (Exception e) {
                throw new BadArgument(xPath);
            }
        });
    }

    private List<String> readFileToList(File file) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private void checkNodeExists(String xml, String xPath) throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException {
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name()));

        NodeList withXPath = ValidationChecker.findWithXPath(stream, xPath);
        Utils.ne(withXPath.getLength(), 0, () -> new BadArgument());
    }

    @Inject
    public void setArclibXmlValidationChecks(@Value("${arclib.arclibXmlValidationChecks}") Resource arclibXmlValidationChecks) {
        this.arclibXmlValidationChecks = arclibXmlValidationChecks;
    }
}
