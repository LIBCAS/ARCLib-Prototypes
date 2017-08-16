package cas.lib.arclib.api;


import cas.lib.arclib.domain.DomainObject;

public interface DataAdapter<T extends DomainObject> {
    Class<T> getType();

    T find(String id);


    Result<T> findAll(Params params);

    T save(T entity);


    void delete(T entity);
}
