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
}
