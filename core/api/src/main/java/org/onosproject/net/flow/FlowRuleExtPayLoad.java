package org.onosproject.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents for 3rd-party private original flow.
 */
public final class FlowRuleExtPayLoad {
    private final byte[] payLoad;

    /**
     * private constructor.
     *
     * @param payLoad private flow
     */
    private FlowRuleExtPayLoad(byte[] payLoad) {
        this.payLoad = payLoad;
    }

    /**
     * Creates a FlowRuleExtPayLoad.
     *
     * @param payLoad payload byte data
     * @return FlowRuleExtPayLoad payLoad
     */
    public static FlowRuleExtPayLoad flowRuleExtPayLoad(byte[] payLoad) {
        return new FlowRuleExtPayLoad(payLoad);
    }

    /**
     * Returns private flow.
     *
     * @return payLoad private flow
     */
    public byte[] payLoad() {
        return payLoad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payLoad);
    }

    public int hash() {
        return Objects.hash(payLoad);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowRuleExtPayLoad) {
            FlowRuleExtPayLoad that = (FlowRuleExtPayLoad) obj;
            return Arrays.equals(payLoad, that.payLoad);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("payLoad", payLoad).toString();
    }
}
