package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;

/**
 * Base abstraction of Karaf shell commands.
 */
public abstract class AbstractShellCommand extends OsgiCommandSupport {

    @Option(name = "-j", aliases = "--json", description = "Output JSON",
            required = false, multiValued = false)
    private boolean json = false;

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     * @throws org.onlab.osgi.ServiceNotFoundException if service is unavailable
     */
    public static <T> T get(Class<T> serviceClass) {
        return DefaultServiceDirectory.getService(serviceClass);
    }

    /**
     * Prints the arguments using the specified format.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    /**
     * Prints the arguments using the specified format to error stream.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void error(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    /**
     * Executes this command.
     */
    protected abstract void execute();

    /**
     * Indicates whether JSON format should be output.
     *
     * @return true if JSON is requested
     */
    protected boolean outputJson() {
        return json;
    }

    @Override
    protected Object doExecute() throws Exception {
        try {
            execute();
        } catch (ServiceNotFoundException e) {
            error(e.getMessage());
        }
        return null;
    }

}
