package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.domain.ArchivalObject;

public class StillProcessingException extends StateException {

    public StillProcessingException(ArchivalObject obj) {
        super(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
