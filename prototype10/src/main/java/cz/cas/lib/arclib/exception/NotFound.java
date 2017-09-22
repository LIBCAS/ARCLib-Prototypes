package cz.cas.lib.arclib.exception;

import org.aspectj.weaver.ast.Not;

public class NotFound extends Exception {

    public NotFound(){};

    public NotFound(String message) {
        super(message);
    }

    public NotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFound(Throwable cause) {
        super(cause);
    }
}
