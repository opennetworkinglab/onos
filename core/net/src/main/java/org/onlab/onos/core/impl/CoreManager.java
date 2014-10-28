package org.onlab.onos.core.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.ApplicationIdStore;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.core.Version;
import org.onlab.util.Tools;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Core service implementation.
 */
@Component
@Service
public class CoreManager implements CoreService {

    private static final File VERSION_FILE = new File("../VERSION");
    private static Version version = Version.version("1.0.0-SNAPSHOT");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore applicationIdStore;

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

    @Override
    public Set<ApplicationId> getAppIds() {
        return applicationIdStore.getAppIds();
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return applicationIdStore.getAppId(id);
    }

    @Override
    public ApplicationId registerApplication(String name) {
        checkNotNull(name, "Application ID cannot be null");
        return applicationIdStore.registerApplication(name);
    }

}
