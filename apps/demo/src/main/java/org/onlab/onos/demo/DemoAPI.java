package org.onlab.onos.demo;

/**
 * Simple demo api interface.
 */
public interface DemoAPI {

    enum InstallType { MESH, RANDOM };

    /**
     * Installs intents based on the installation type.
     * @param type the installation type.
     */
    void setup(InstallType type);

    /**
     * Uninstalls all existing intents.
     */
    void tearDown();

}
