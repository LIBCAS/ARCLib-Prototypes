package cz.inqool.arclib.Util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.util.Optional;
import java.util.function.Supplier;

import static jdk.nashorn.api.scripting.ScriptUtils.unwrap;

public class Utils {

    public static <T extends RuntimeException> void notNull(Object o, Supplier<T> supplier) {
        if (o == null) {
            throw supplier.get();
        } else if (o instanceof Optional) {
            if (!((Optional) o).isPresent()) {
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
}
