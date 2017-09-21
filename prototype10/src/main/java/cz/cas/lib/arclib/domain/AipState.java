package cz.cas.lib.arclib.domain;

import lombok.Getter;

@Getter
public enum AipState {
    PROCESSING,
    ARCHIVED,
    DELETED,
    REMOVED
}
