package org.onlab.onos.net.flow;

/**
 * Factory class to create various traffic selection criteria.
 */
public final class Criteria {

    // Ban construction
    private Criteria() {
    }

    /**
     * Creates a match on ETH_SRC field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param macValue MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthSrc(MACValue macValue) {
        return null;
    }

    /**
     * Creates a match on ETH_DST field using the specified value. This value
     * may be a wildcard mask.
     *
     * @param macValue MAC address value or wildcard mask
     * @return match criterion
     */
    public static Criterion matchEthDst(MACValue macValue) {
        return null;
    }


    // Dummy to illustrate the concept for now; delete ASAP
    private static class MACValue { }
}
