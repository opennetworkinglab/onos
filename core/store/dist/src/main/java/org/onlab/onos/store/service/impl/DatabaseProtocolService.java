package org.onlab.onos.store.service.impl;

import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.spi.protocol.Protocol;

// interface required for connecting DatabaseManager + ClusterMessagingProtocol
// TODO: Consider changing ClusterMessagingProtocol to non-Service class
public interface DatabaseProtocolService extends Protocol<TcpMember> {

}
