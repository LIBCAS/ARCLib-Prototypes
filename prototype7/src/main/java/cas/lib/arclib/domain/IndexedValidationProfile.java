package cas.lib.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "issvp", type = "comment")
public class IndexedValidationProfile extends IndexedDatedObject {

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference document;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference author;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference state;
}
