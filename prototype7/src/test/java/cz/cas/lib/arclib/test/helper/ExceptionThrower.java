package cz.cas.lib.arclib.test.helper;

@FunctionalInterface
public interface ExceptionThrower {
    void throwException() throws Throwable;
}
