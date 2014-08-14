package net.onrc.onos.api.host;

import net.onrc.onos.api.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService {
    
    void hostDetected(HostDescription hostDescription);

}
