package org.onlab.onos.core;

import java.util.Set;

/**
 * Service for interacting with the core system of the controller.
 */
public interface CoreService {

    /**
     * Returns the product version.
     *
     * @return product version
     */
    Version version();

    /**
     * Returns the set of currently registered application identifiers.
     *
     * @return set of application ids
     */
    Set<ApplicationId> getAppIds();

    /**
     * Returns an existing application id from a given id.
     * @param id the short value of the id
     * @return an application id
     */
    ApplicationId getAppId(Short id);

    /**
     * Registers a new application by its name, which is expected
     * to follow the reverse DNS convention, e.g.
     * {@code org.flying.circus.app}
     *
     * @param identifier string identifier
     * @return the application id
     */
    ApplicationId registerApplication(String identifier);

}
