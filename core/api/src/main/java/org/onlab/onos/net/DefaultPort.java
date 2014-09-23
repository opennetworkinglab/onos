package org.onlab.onos.net;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Default port implementation.
 */
public class DefaultPort implements Port {

    private final Element element;
    private final PortNumber number;
    private final boolean isEnabled;

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param element     parent network element
     * @param number      port number
     * @param isEnabled   indicator whether the port is up and active
     */
    public DefaultPort(Element element, PortNumber number,
                       boolean isEnabled) {
        this.element = element;
        this.number = number;
        this.isEnabled = isEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, isEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPort) {
            final DefaultPort other = (DefaultPort) obj;
            return Objects.equals(this.element.id(), other.element.id()) &&
                    Objects.equals(this.number, other.number) &&
                    Objects.equals(this.isEnabled, other.isEnabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element.id())
                .add("number", number)
                .add("isEnabled", isEnabled)
                .toString();
    }

    @Override
    public PortNumber number() {
        return number;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Element element() {
        return element;
    }

}
