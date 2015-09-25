package org.onosproject.incubator.net.virtual;

import org.onosproject.net.provider.ProviderService;

/**
 * Service through which virtual network providers can inject information into
 * the core.
 */
public interface VirtualNetworkProviderService extends ProviderService<VirtualNetworkProvider> {
    // TODO: Add methods for notification of core about damaged tunnels, etc.
}
