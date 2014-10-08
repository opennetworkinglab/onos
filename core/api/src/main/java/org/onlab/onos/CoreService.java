package org.onlab.onos;

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

}
