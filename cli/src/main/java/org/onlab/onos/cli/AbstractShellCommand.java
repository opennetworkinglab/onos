package org.onlab.onos.cli;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Base abstraction of Karaf shell commands.
 */
public abstract class AbstractShellCommand extends OsgiCommandSupport {

    /**
     * Returns the reference to the implementaiton of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     */
    static <T> T get(Class<T> serviceClass) {
        BundleContext bc = FrameworkUtil.getBundle(AbstractShellCommand.class).getBundleContext();
        return bc.getService(bc.getServiceReference(serviceClass));
    }

}
