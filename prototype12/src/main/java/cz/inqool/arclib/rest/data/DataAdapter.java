package cz.inqool.arclib.rest.data;

import cz.inqool.arclib.domain.DomainObject;
import cz.inqool.arclib.index.dto.Params;
import cz.inqool.arclib.index.dto.Result;

public interface DataAdapter<T extends DomainObject> {
    Class<T> getType();

    T find(String id);


    Result<T> findAll(Params params);

    T save(T entity);


    void delete(T entity);
}
