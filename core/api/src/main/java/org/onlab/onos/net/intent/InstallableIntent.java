package org.onlab.onos.net.intent;

import org.onlab.onos.net.Link;

import java.util.Collection;

/**
 * Abstraction of an intent that can be installed into
 * the underlying system without additional compilation.
 */
public interface InstallableIntent extends Intent {

    /**
     * Returns the collection of links that are required for this installable
     * intent to exist.
     *
     * @return collection of links
     */
    // FIXME: replace this with 'NetworkResource'
    Collection<Link> requiredLinks();

}
