package cz.inqool.arclib;

public class SIPAntivirusScannerException extends Exception {
    public SIPAntivirusScannerException(String errOutput) {
        super(errOutput);
    }
}
