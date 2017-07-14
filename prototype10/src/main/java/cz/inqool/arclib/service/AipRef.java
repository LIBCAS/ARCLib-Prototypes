package cz.inqool.arclib.service;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AipRef {
    @Setter
    @Getter
    private FileRef sip;

    private List<FileRef> xmls = new ArrayList<>();

    public List<FileRef> getXmls() {
        return Collections.unmodifiableList(xmls);
    }

    public FileRef getXml(int index) {
        return xmls.get(index);
    }

    public void addXml(FileRef xml) {
        xmls.add(xml);
    }
}
