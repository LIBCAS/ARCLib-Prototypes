package cz.inqool.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Setter
@Getter
@AllArgsConstructor
public class FileRef {
    private String id;
    private String name;
    private InputStream stream;
}
