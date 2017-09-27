package cz.cas.lib.arclib.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AipCreationMd5Info {
    private String sipMd5;
    private String xmlMd5;
}
