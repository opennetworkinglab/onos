package org.onlab.onos.net;

import java.util.Set;

/**
 * Represents an set of simply key/value string annotations.
 */
public interface SparseAnnotations extends Annotations {

    /**
     * {@inheritDoc}
     * <p/>
     * Note that this set includes keys for any attributes tagged for removal.
     */
    @Override
    public Set<String> keys();

    /**
     * Indicates whether the specified key has been tagged as removed. This is
     * used to for merging sparse annotation sets.
     *
     * @param key annotation key
     * @return true if the previous annotation has been tagged for removal
     */
    public boolean isRemoved(String key);

}
