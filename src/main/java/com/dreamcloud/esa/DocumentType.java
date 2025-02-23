package com.dreamcloud.esa;

public enum DocumentType {
    DREAM("dream"),
    WIKI("wiki");

    public final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public static DocumentType valueOfLabel(String label) {
        for (DocumentType e : values()) {
            if (e.label.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
