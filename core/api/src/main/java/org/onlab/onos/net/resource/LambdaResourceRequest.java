package org.onlab.onos.net.resource;

/**
 * Representation of a request for lambda resource.
 */
public class LambdaResourceRequest implements ResourceRequest {

    @Override
    public ResourceType type() {
        return ResourceType.LAMBDA;
    }

}
