package org.onlab.onos.net.resource;

import java.util.Set;

/**
 * Abstraction of a resources of a link.
 */
public interface LinkResources {

    /**
     * Returns resources as a set of {@link LinkResource}s.
     *
     * @return a set of {@link LinkResource}s
     */
    Set<LinkResource> resources();

    /**
     * Builder of {@link LinkResources}.
     */
    public interface Builder {

        /**
         * Adds bandwidth resource.
         * <p>
         * This operation adds given bandwidth to previous bandwidth and
         * generates single bandwidth resource.
         *
         * @param bandwidth bandwidth value to be added
         * @return self
         */
        public Builder addBandwidth(double bandwidth);

        /**
         * Adds lambda resource.
         *
         * @param lambda lambda value to be added
         * @return self
         */
        public Builder addLambda(int lambda);

        /**
         * Builds an immutable link resources.
         *
         * @return link resources
         */
        public LinkResources build();
    }
}
