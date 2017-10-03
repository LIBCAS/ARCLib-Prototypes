package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.ArchivalObject;

public class StateException extends Exception {
    private ArchivalObject obj;

    public StateException() {

    }

    public StateException(ArchivalObject obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        if (obj instanceof AipSip) {
            AipSip sip = ((AipSip) obj);
            return String.format("Can't perform operation SIP: %s is in state %s.", sip.getId(), sip.getState().toString());
        } else if (obj instanceof AipXml) {
            AipXml xml = ((AipXml) obj);
            return String.format("Can't perform operation XML version: %d of SIP: %s is in state %s.", xml.getVersion(), xml.getSip().getId(), xml.getState().toString());
        } else
            return "Can't perform operation.";
    }
}
