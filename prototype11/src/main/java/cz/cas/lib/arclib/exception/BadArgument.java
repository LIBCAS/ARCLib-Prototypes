package cz.cas.lib.arclib.exception;


public class BadArgument extends GeneralException {

    public BadArgument(String e) {
        super(e);
    }

    public BadArgument(String e,Throwable cause) {
        super(e,cause);
    }

    public BadArgument(Throwable cause) {
        super(cause);
    }
}
