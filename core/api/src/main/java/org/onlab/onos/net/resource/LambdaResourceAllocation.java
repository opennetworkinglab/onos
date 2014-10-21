package org.onlab.onos.net.resource;

/**
 * Representation of allocated lambda resource.
 */
public interface LambdaResourceAllocation extends LambdaResourceRequest {
    /**
     * Returns the lambda resource.
     *
     * @return the lambda resource
     */
    Lambda lambda();
}
