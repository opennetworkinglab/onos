package org.onosproject.pcelabelstore;

/**
 * Representation of label operation over PCEP.
 */
public enum PcepLabelOp {
    /**
     * Signifies that the label operation is addition.
     */
    ADD,

    /**
     * Signifies that the label operation is modification. This is reserved for future.
     */
    MODIFY,

    /**
     * Signifies that the label operation is deletion.
     */
    REMOVE
}
