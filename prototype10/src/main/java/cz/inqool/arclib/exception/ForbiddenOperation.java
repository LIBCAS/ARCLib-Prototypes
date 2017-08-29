package cz.inqool.arclib.exception;

import cz.inqool.arclib.domain.DomainObject;

public class ForbiddenOperation extends GeneralException {
    private Object object;

    public ForbiddenOperation(Object object) {
        super();
        this.object = object;
    }

    public ForbiddenOperation(Class<?> clazz, String objectId) {
        super();
        try {
            this.object = clazz.newInstance();

            if (DomainObject.class.isAssignableFrom(clazz)) {
                ((DomainObject) this.object).setId(objectId);
            }

        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String toString() {
        return "ForbiddenOperation{" +
                "object=" + object +
                '}';
    }

    public Object getObject() {
        return object;
    }
}
