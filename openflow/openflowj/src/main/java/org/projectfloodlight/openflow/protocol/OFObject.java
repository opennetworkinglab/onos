package org.projectfloodlight.openflow.protocol;

import org.projectfloodlight.openflow.types.PrimitiveSinkable;


/**
 * Base interface of all OpenFlow objects (e.g., messages, actions, stats, etc.)
 */
public interface OFObject extends Writeable, PrimitiveSinkable {
    OFVersion getVersion();
}
