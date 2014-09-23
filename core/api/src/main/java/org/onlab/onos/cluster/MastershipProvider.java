package org.onlab.onos.cluster;

import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a mastership information provider.
 */
public interface MastershipProvider extends Provider {
    // do we get role info from the local OFcontroller impl?
    // needs to also read from distributed store and emit events?
    // roleChanged(DeviceId deviceId, MastershipRole newRole);
}
