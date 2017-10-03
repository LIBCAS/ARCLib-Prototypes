package cz.cas.lib.arclib.domain;

import lombok.Getter;

@Getter
public enum XmlState {
    ARCHIVED,
    PROCESSING,
    ROLLBACKED
}
