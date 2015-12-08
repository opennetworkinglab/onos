package org.onosproject.provider.lldp.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.basics.BasicFeatureConfig;

/**
 * A feature to send and receive probes carrying a cluster-unique fingerprint.
 * Note that, as it leverages LinkDiscovery, disabling linkDiscovery will disable
 * this function.
 */
public class FingerprintProbeFromDevice extends BasicFeatureConfig<DeviceId> {

    protected FingerprintProbeFromDevice() {
        // default:disabled
        super(false);
    }

}
