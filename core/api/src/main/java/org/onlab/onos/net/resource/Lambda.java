package org.onlab.onos.net.resource;

import java.util.Objects;

/**
 * Representation of lambda resource.
 */
public final class Lambda extends LinkResource {

    private final int lambda;

    /**
     * Creates a new instance with given lambda.
     *
     * @param lambda lambda value to be assigned
     */
    private Lambda(int lambda) {
        this.lambda = lambda;
    }

    /**
     * Creates a new instance with given lambda.
     *
     * @param lambda lambda value to be assigned
     * @return {@link Lambda} instance with given lambda
     */
    public static Lambda valueOf(int lambda) {
        return new Lambda(lambda);
    }

    /**
     * Returns lambda as an int value.
     * @return lambda as an int value
     */
    public int toInt() {
        return lambda;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Lambda) {
            Lambda that = (Lambda) obj;
            return Objects.equals(this.lambda, that.lambda);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.lambda);
    }

    @Override
    public String toString() {
        return String.valueOf(this.lambda);
    }

}
