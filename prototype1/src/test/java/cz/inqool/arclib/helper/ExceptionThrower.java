package cz.inqool.arclib.helper;

@FunctionalInterface
public interface ExceptionThrower {
    void throwException() throws Throwable;
}
