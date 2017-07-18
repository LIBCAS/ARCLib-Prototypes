package cz.inqool.arclib.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StoredFileInfoDto {
    private String id;
    private String name;
    private boolean consistent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoredFileInfoDto that = (StoredFileInfoDto) o;

        if (isConsistent() != that.isConsistent()) return false;
        if (!getId().equals(that.getId())) return false;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + (isConsistent() ? 1 : 0);
        return result;
    }
}
