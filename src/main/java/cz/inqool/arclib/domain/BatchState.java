package cz.inqool.arclib.domain;

import lombok.Getter;

@Getter
public enum BatchState {
    PROCESSING,
    SUSPENDED,
    CANCELED
}
