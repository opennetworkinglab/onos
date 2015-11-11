package org.onoproject.yangtool;

import org.apache.commons.configuration.ConfigurationException;

import java.io.IOException;

/**
 * Main of the uangloader tool in order to be called through command line.
 */
public class YangLoaderMain {
    public static void main (String args []) throws IOException,
            ConfigurationException, InterruptedException {
        YangLoader yl = new YangLoader();
        yl.generateBehaviourInterface(args[0], args[1]);
    }
}
