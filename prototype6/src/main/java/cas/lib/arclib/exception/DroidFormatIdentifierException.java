package cas.lib.arclib.exception;

public class DroidFormatIdentifierException extends Exception {
    /**
     * Threw when error occurs during the format identify process
     *
     * @param errOutput
     */
    public DroidFormatIdentifierException(String errOutput) {
        super(errOutput);
    }
}
