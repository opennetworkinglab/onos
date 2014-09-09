package org.onlab.onos;

/**
 * Example of a simple service that provides greetings and it
 * remembers the names which were greeted.
 */
public interface GreetService {

    /**
     * Returns a greeting tailored to the specified name.
     *
     * @param name some name
     * @return greeting
     */
    String yo(String name);

    /**
     * Returns an iterable of names encountered thus far.
     *
     * @return iterable of names
     */
    Iterable<String> names();
}
