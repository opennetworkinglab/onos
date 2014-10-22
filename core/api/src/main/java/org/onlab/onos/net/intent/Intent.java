package org.onlab.onos.net.intent;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.NetworkResource;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of an application level intent.
 * <p/>
 * Make sure that an Intent should be immutable when a new type is defined.
 */
public abstract class Intent implements BatchOperationTarget {

    private final IntentId id;
    private final ApplicationId appId;
    private final Collection<NetworkResource> resources;

    /**
     * Constructor for serializer.
     */
    protected Intent() {
        this.id = null;
        this.appId = null;
        this.resources = null;
    }

    /**
     * Creates a new intent.
     *
     * @param id        intent identifier
     * @param appId     application identifier
     * @param resources required network resources (optional)
     */
    protected Intent(IntentId id, ApplicationId appId,
                     Collection<NetworkResource> resources) {
        this.id = checkNotNull(id, "Intent ID cannot be null");
        this.appId = checkNotNull(appId, "Application ID cannot be null");
        this.resources = resources;
    }

    /**
     * Returns the intent identifier.
     *
     * @return intent fingerprint
     */
    public IntentId id() {
        return id;
    }

    /**
     * Returns the identifier of the application that requested the intent.
     *
     * @return application identifier
     */
    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns the collection of resources required for this intent.
     *
     * @return collection of resources; may be null
     */
    public Collection<NetworkResource> resources() {
        return resources;
    }

    /**
     * Produces an intent identifier backed by hash-like fingerprint for the
     * specified class of intent and its constituent fields.
     *
     * @param fields intent fields
     * @return intent identifier
     */
    protected static IntentId id(Object... fields) {
        // FIXME: spread the bits across the full long spectrum
        return IntentId.valueOf(Objects.hash(fields));
    }

    /**
     * Indicates whether or not the intent is installable.
     *
     * @return true if installable
     */
    public boolean isInstallable() {
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Intent other = (Intent) obj;
        return Objects.equals(this.id, other.id);
    }

}
