package cz.inqool.arclib;

import lombok.Getter;
import cz.inqool.arclib.index.Labeled;

/**
 * Type of script
 */
@Getter
public enum ScriptType implements Labeled {
    SHELL("shell");

    private String label;

    ScriptType(String label) {
        this.label = label;
    }
}
