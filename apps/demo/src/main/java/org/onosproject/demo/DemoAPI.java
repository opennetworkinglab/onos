package org.onlab.onos.demo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Simple demo api interface.
 */
public interface DemoAPI {

    enum InstallType { MESH, RANDOM };

    /**
     * Installs intents based on the installation type.
     * @param type the installation type.
     * @param runParams run params
     */
    void setup(InstallType type, Optional<JsonNode> runParams);

    /**
     * Uninstalls all existing intents.
     */
    void tearDown();

}
