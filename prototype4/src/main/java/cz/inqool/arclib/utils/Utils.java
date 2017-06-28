package cz.inqool.arclib.utils;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.util.Optional;
import java.util.function.Supplier;

public class Utils {

    public static String bytesToHexString(byte[] bytes){
        final StringBuilder builder = new StringBuilder();
        for(byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    };

    public static <T extends RuntimeException> void notNull(Object o, Supplier<T> supplier) {
        if (o == null) {
            throw supplier.get();
        } else if (o instanceof Optional) {
            if(!((Optional)o).isPresent()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) == null) {
                throw supplier.get();
            }
        }
    }
    public static boolean isProxy(Object a) {
        return (AopUtils.isAopProxy(a) && a instanceof Advised);
    }
    public static <T> T unwrap(T a) {
        if(isProxy(a)) {
            try {
                return (T) ((Advised)a).getTargetSource().getTarget();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return a;
        }
    }
}
