package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.domain.ArchivalObject;

public class DeletedException extends StateException {

    public DeletedException(ArchivalObject obj) {
        super(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
