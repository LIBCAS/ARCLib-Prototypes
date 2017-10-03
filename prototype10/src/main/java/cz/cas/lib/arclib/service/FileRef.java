package cz.cas.lib.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

/**
 * Transfer class for file data used for both SIP and XML files. When used with SIP, {@code version} is typically not set.
 */
@Setter
@Getter
@AllArgsConstructor
public class FileRef {
    private String id;
    private int version;
    private InputStream stream;

    public FileRef(String id, InputStream fileStream) {
        this.id = id;
        this.stream = fileStream;
    }

    public FileRef(int version, InputStream fileStream) {
        this.version = version;
        this.stream = fileStream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileRef fileRef = (FileRef) o;

        return getId().equals(fileRef.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}


