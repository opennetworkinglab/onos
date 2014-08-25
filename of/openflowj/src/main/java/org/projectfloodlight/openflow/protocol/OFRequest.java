package org.projectfloodlight.openflow.protocol;

/** Type safety interface. Enables type safe combinations of requests and replies */
public interface OFRequest<REPLY extends OFMessage> extends OFMessage {
}
