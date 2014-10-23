package org.onlab.onos.net.resource;

/**
 * Representation of allocated lambda resource.
 */
public class LambdaResourceAllocation extends LambdaResourceRequest
        implements ResourceAllocation {
    private final Lambda lambda;

    @Override
    public ResourceType type() {
        return ResourceType.LAMBDA;
    }

    /**
     * Creates a new {@link LambdaResourceAllocation} with {@link Lambda}
     * object.
     *
     * @param lambda allocated lambda
     */
    public LambdaResourceAllocation(Lambda lambda) {
        this.lambda = lambda;
    }

    /**
     * Returns the lambda resource.
     *
     * @return the lambda resource
     */
    public Lambda lambda() {
        return lambda;
    }
}
