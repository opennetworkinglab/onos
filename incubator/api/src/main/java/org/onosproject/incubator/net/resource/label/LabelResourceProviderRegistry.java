package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.provider.ProviderRegistry;

/**
 * Abstraction of an label resource provider registry.
 */
@Beta
public interface LabelResourceProviderRegistry
        extends ProviderRegistry<LabelResourceProvider, LabelResourceProviderService> {

}
