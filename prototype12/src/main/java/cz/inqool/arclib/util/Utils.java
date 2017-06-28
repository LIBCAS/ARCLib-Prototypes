package cz.inqool.arclib.util;

import cz.inqool.arclib.domain.DomainObject;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import cz.inqool.arclib.index.Labeled;
import cz.inqool.arclib.index.LabeledReference;
import cz.inqool.arclib.domain.DictionaryObject;
import cz.inqool.arclib.index.IndexedDictionaryObject;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.text.Normalizer;

public class Utils {

    public static <T> T unwrap(T a) {
        if (isProxy(a)) {
            try {
                return (T) ((Advised) a).getTargetSource().getTarget();
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

    public static <T extends DomainObject> List<T> sortByIdList(List<String> ids, Iterable<T> objects) {
        Map<String, T> map = StreamSupport.stream(objects.spliterator(), true)
                .collect(Collectors.toMap(DomainObject::getId, o -> o));

        return ids.stream()
                .map(map::get)
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    public static String normalize(String s) {
        if (s != null) {
            return stripAccents(s).toLowerCase();
        } else {
            return null;
        }
    }

    public static String stripAccents(String s) {
        if (s != null) {
            s = Normalizer.normalize(s, Normalizer.Form.NFD);
            s = s.replaceAll("[^\\p{ASCII}]", "");
            return s;
        } else {
            return null;
        }
    }

    public static <T extends DomainObject> LabeledReference toLabeledReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static <T extends DictionaryObject> LabeledReference toLabeledReference(T obj) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static <T extends Labeled> LabeledReference toLabeledReference(T obj) {
        if (obj != null) {
            return new LabeledReference(obj.name(), obj.getLabel());
        } else {
            return null;
        }
    }

    public static <T extends Enum> LabeledReference toLabeledReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new LabeledReference(obj.toString(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static LabeledReference toLabeledReference(IndexedDictionaryObject obj) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static <T> Set<T> asSet(Collection<T> a) {
        return new HashSet<>(a);
    }

    public static <T> Set<T> asSet(T... a) {
        return new HashSet<>(Arrays.asList(a));
    }

    public static <T> List<T> asList(Collection<T> a) {
        return a.stream().collect(Collectors.toList());
    }

    public static <T> List<T> asList(T... a) {
        return Arrays.asList(a);
    }

    public static <T> T[] asArray(T... a) {
        return a;
    }

    public static <T> List<T> asList(Collection<T> base, T... a) {
        List<T> list = new ArrayList<>(base);
        list.addAll(Arrays.asList(a));

        return list;
    }

    public static <U, T extends RuntimeException> void eq(U o1,  U o2, Supplier<T> supplier) {
        if (!Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }
}
