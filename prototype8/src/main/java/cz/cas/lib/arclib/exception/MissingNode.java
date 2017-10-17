package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.exception.general.GeneralException;

public class MissingNode extends GeneralException {
    private String xPath;

    private String xml;

    public MissingNode(String xPath, String xml) {
        super();
        this.xPath = xPath;
        this.xml = xml;
    }

    @Override
    public String toString() {
        return "MissingNode{" +
                "xpath=" + xPath +
                ", xml='" + xml + '\'' +
                '}';
    }
}
