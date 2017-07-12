package cz.inqool.uas.util;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.GeneralException;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public static boolean isProxy(Object a) {
        return (AopUtils.isAopProxy(a) && a instanceof Advised);
    }


    public static <T extends DomainObject> List<T> sortByIdList(List<String> ids, Iterable<T> objects) {
        Map<String, T> map = StreamSupport.stream(objects.spliterator(), true)
                .collect(Collectors.toMap(DomainObject::getId, o -> o));

        return ids.stream()
                .map(map::get)
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface Checked {
        void checked() throws Exception;
    }

    public static void checked(Checked method) {
        try {
            method.checked();
        } catch (Exception ex) {
            if (ex instanceof GeneralException) {
                throw (GeneralException)ex;
            } else {
                throw new GeneralException(ex);
            }

        }
    }

    public static <T extends RuntimeException> void checked(Checked method, Supplier<T> supplier ) {
        try {
            method.checked();
        } catch (Exception ex) {
            throw supplier.get();
        }
    }
}
