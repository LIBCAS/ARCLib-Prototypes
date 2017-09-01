package cz.cas.lib.arclib.exception;

public class ExporterException extends Exception {
    public ExporterException() {
    }

    public ExporterException(String message) {
        super(message);
    }

    public ExporterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExporterException(Throwable cause) {
        super(cause);
    }
}
