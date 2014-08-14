package net.onrc.onos.of.ctl.registry;



public class ControllerRegistryEntry implements Comparable<ControllerRegistryEntry> {
    //
    // TODO: Refactor the implementation and decide whether controllerId
    // is needed. If "yes", we might need to consider it inside the
    // compareTo(), equals() and hashCode() implementations.
    //
    private final String controllerId;
    private final int sequenceNumber;

    public ControllerRegistryEntry(String controllerId, int sequenceNumber) {
        this.controllerId = controllerId;
        this.sequenceNumber = sequenceNumber;
    }

    public String getControllerId() {
        return controllerId;
    }

    /**
     * Compares this object with the specified object for order.
     * NOTE: the test is based on ControllerRegistryEntry sequence numbers,
     * and doesn't include the controllerId.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(ControllerRegistryEntry o) {
        return this.sequenceNumber - o.sequenceNumber;
    }

    /**
     * Test whether some other object is "equal to" this one.
     * NOTE: the test is based on ControllerRegistryEntry sequence numbers,
     * and doesn't include the controllerId.
     *
     * @param obj the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false
     * otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ControllerRegistryEntry) {
            ControllerRegistryEntry other = (ControllerRegistryEntry) obj;
            return this.sequenceNumber == other.sequenceNumber;
        }
        return false;
    }

    /**
     * Get the hash code for the object.
     * NOTE: the computation is based on ControllerRegistryEntry sequence
     * numbers, and doesn't include the controller ID.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Integer.valueOf(this.sequenceNumber).hashCode();
    }
}
