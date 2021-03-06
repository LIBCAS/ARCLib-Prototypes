package cz.cas.lib.arclib;

public class SIPAntivirusScannerException extends Exception {
    /**
     * Threw when error occurs during the antivirus scan process
     *
     * @param errOutput
     */
    public SIPAntivirusScannerException(String errOutput) {
        super(errOutput);
    }
}
