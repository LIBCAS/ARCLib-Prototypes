package cz.cas.lib.arclib.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IndexFieldConfig {
    private String fieldName;
    private String xpath;
    private boolean fullText;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexFieldConfig that = (IndexFieldConfig) o;

        return getFieldName().equals(that.getFieldName());
    }

    @Override
    public int hashCode() {
        return getFieldName().hashCode();
    }
}
