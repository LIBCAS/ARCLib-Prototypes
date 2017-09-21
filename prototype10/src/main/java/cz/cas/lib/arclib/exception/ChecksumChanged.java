package cz.cas.lib.arclib.exception;

public class ChecksumChanged extends Exception {

    public ChecksumChanged(){};

    public ChecksumChanged(String message) {
        super(message);
    }

    public ChecksumChanged(String message, Throwable cause) {
        super(message, cause);
    }

    public ChecksumChanged(Throwable cause) {
        super(cause);
    }
}
