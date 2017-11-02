package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.general.DomainObject;
import cz.cas.lib.arclib.exception.general.GeneralException;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public static InputStream stringToInputStream(String text) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name()));
    }

    public static String nodeToString(org.w3c.dom.Node node) throws TransformerException {
        StringWriter sw = new StringWriter();

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));

        return sw.toString();
    }

    public static <T extends DomainObject> List<T> sortByIdList(List<String> ids, Iterable<T> objects) {
        Map<String, T> map = StreamSupport.stream(objects.spliterator(), true)
                .collect(Collectors.toMap(DomainObject::getId, o -> o));

        return ids.stream()
                .map(map::get)
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    public static <U, T extends RuntimeException> void ne(U o1, U o2, Supplier<T> supplier) {
        if (Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void eq(U o1, U o2, Supplier<T> supplier) {
        if (!Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static class Pair<L,R> {
        private L l;
        private R r;
        public Pair(L l, R r){
            this.l = l;
            this.r = r;
        }
        public L getL(){ return l; }
        public R getR(){ return r; }
        public void setL(L l){ this.l = l; }
        public void setR(R r){ this.r = r; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair<?, ?> pair = (Pair<?, ?>) o;

            if (l != null ? !l.equals(pair.l) : pair.l != null) return false;
            return r != null ? r.equals(pair.r) : pair.r == null;
        }

        @Override
        public int hashCode() {
            int result = l != null ? l.hashCode() : 0;
            result = 31 * result + (r != null ? r.hashCode() : 0);
            return result;
        }
    }

    public static File[] listFilesMatchingRegex(File root, String regex) throws FileNotFoundException {
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(root + " is no directory.");
        }
        final Pattern p = Pattern.compile(regex); // careful: could also throw an exception!
        return root.listFiles(file -> p.matcher(file.getName()).matches());
    }

    public static List<String> readLinesOfFileToList(File file) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
