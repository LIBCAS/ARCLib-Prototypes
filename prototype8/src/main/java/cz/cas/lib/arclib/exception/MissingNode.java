package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.exception.general.GeneralException;

public class MissingNode extends GeneralException {
    private String xPath;

    public MissingNode(String xPath) {
        super();
        this.xPath = xPath;
    }

    @Override
    public String toString() {
        return "MissingNode{" +
                "xpath=" + xPath +
                '}';
    }

    public String getxPath() {
        return xPath;
    }
}
