package org.onosproject.net.tunnel;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import java.util.Optional;

import org.onosproject.net.AbstractModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.ElementId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Default label model implementation.
 */
public class DefaultLabel extends AbstractModel implements Label {
    private final Optional<ElementId> elementId;
    private final Optional<PortNumber> portNumber;
    private final Optional<Label> parentLabel;
    private final Type type;
    private final LabelId id;
    private final boolean isGlobal;

    /**
     * Creates a label attributed to the specified provider (may be null).
     * if provider is null, which means the label is not managed by the SB.
     *
     * @param elementId     parent network element
     * @param number      port number
     * @param parentLabel parent port or parent label
     * @param type        port type
     * @param id          LabelId
     * @param isGlobal    indicator whether the label is global significant or not
     * @param annotations optional key/value annotations
     */
    public DefaultLabel(ProviderId providerId, Optional<ElementId> elementId,
                        Optional<PortNumber> number, Optional<Label> parentLabel,
                        Type type, LabelId id, boolean isGlobal, Annotations... annotations) {
        super(providerId, annotations);
        this.elementId = elementId;
        this.portNumber = number;
        this.parentLabel = parentLabel;
        this.id = id;
        this.type = type;
        this.isGlobal = isGlobal;
    }

    @Override
    public LabelId id() {
        return id;
    }

    @Override
    public Optional<ElementId> elementId() {
        return elementId;
    }

    @Override
    public Optional<PortNumber> portNumber() {
        return portNumber;
    }

    @Override
    public Optional<Label> parentLabel() {
        return parentLabel;
    }

    @Override
    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, portNumber, parentLabel, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultLabel) {
            final DefaultLabel other = (DefaultLabel) obj;
            return Objects.equals(this.id, other.id) &&
                   Objects.equals(this.type, other.type) &&
                   Objects.equals(this.isGlobal, other.isGlobal) &&
                   Objects.equals(this.elementId, other.elementId) &&
                   Objects.equals(this.portNumber, other.portNumber) &&
                   Objects.equals(this.parentLabel, other.parentLabel);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("elementId", elementId)
                .add("portNumber", portNumber)
                .add("parentLabel", parentLabel)
                .add("type", type)
                .add("id", id)
                .add("isGlobal", isGlobal)
                .toString();
    }

}
