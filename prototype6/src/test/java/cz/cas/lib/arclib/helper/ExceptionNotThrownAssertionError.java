package cz.cas.lib.arclib.helper;

public class ExceptionNotThrownAssertionError extends AssertionError {
    public ExceptionNotThrownAssertionError() {
        super("Expected exception was not thrown.");
    }
}
