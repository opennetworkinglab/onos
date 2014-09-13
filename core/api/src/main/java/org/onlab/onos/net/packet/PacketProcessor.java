package org.onlab.onos.net.packet;


/**
 * Abstraction of an inbound packet processor.
 */
public interface PacketProcessor {

    public static final int ADVISOR_MAX = Integer.MAX_VALUE / 3;
    public static final int DIRECTOR_MAX = (Integer.MAX_VALUE / 3) * 2;
    public static final int OBSERVER_MAX = Integer.MAX_VALUE;

    /**
     * Processes the inbound packet as specified in the given context.
     *
     * @param context packet processing context
     */
    void process(PacketContext context);

}
