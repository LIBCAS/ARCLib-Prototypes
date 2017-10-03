package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.domain.ArchivalObject;

public class RollbackedException extends StateException {

    public RollbackedException(ArchivalObject obj){
        super(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
