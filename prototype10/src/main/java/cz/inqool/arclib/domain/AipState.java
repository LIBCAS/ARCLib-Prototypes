package cz.inqool.arclib.domain;

import lombok.Getter;

@Getter
public enum AipState {
    PROCESSING,
    ARCHIVED,
    DELETED,
    CORRUPTED,
    REMOVED
}
