package org.onlab.onos.cli;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Base abstraction of Karaf shell commands.
 */
public abstract class AbstractShellCommand extends OsgiCommandSupport {

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     */
    public static <T> T get(Class<T> serviceClass) {
        BundleContext bc = FrameworkUtil.getBundle(AbstractShellCommand.class).getBundleContext();
        return bc.getService(bc.getServiceReference(serviceClass));
    }

    /**
     * Prints the arguments using the specified format.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public static void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

}
