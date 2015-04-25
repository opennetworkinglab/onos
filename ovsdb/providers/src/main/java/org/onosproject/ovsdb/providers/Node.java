package org.onosproject.ovsdb.providers;

import java.io.Serializable;

/**
 * OVSDB Node Information.
 */
public class Node implements Serializable {
    private static final long serialVersionUID = 1L;
    private Object nodeID;
    private String nodeType;
    private String nodeIDString;

    public Node(Object nodeID, String nodeType, String nodeIDString) {
        super();
        this.nodeID = nodeID;
        this.nodeType = nodeType;
        this.nodeIDString = nodeIDString;
    }

}
