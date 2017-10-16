package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.IndexStore;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IndexApi {

    private IndexStore indexStore;

    @RequestMapping(value="/list", method = RequestMethod.GET)
    public List<String> listDocumentIds(FilterWrapper filter){
        FilterWrapper f2 = filter;
        return indexStore.findAll(filter.getFilter());
    }

    @Inject
    public void setIndexStore(IndexStore indexStore){
        this.indexStore=indexStore;
    }
}
