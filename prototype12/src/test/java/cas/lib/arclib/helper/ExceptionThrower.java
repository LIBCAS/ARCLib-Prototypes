package cas.lib.arclib.helper;

@FunctionalInterface
public interface ExceptionThrower {
    void throwException() throws Throwable;
}
