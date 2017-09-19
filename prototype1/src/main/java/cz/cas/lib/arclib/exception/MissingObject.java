package cz.cas.lib.arclib.exception;

public class MissingObject extends GeneralException {
    private String type;
    private String id;

    public MissingObject(Class clazz, String id) {
        this.type = clazz.getTypeName();
        this.id = id;
    }

    public MissingObject(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String toString() {
        return "MissingObject{type=" + this.type + ", id='" + this.id + '\'' + '}';
    }

    public String getId() {
        return this.id;
    }
}
