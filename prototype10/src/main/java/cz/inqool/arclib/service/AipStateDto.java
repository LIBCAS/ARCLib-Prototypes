package cz.inqool.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AipStateDto {
    private boolean stored;
    private boolean consistent;
}

