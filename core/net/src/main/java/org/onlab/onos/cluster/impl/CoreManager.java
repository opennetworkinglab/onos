package org.onlab.onos.cluster.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.CoreService;
import org.onlab.onos.Version;
import org.onlab.util.Tools;

import java.io.File;
import java.util.List;

/**
 * Core service implementation.
 */
@Component
@Service
public class CoreManager implements CoreService {

    private static final File VERSION_FILE = new File("../VERSION");
    private static Version version = Version.version("1.0.0-SNAPSHOT");

    // TODO: work in progress

    @Activate
    public void activate() {
        List<String> versionLines = Tools.slurp(VERSION_FILE);
        if (versionLines != null && !versionLines.isEmpty()) {
            version = Version.version(versionLines.get(0));
        }
    }

    @Override
    public Version version() {
        return version;
    }

}
