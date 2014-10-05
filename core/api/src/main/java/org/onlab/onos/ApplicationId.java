package org.onlab.onos;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application id generator class.
 */
public final class ApplicationId {

    private static final AtomicInteger ID_DISPENCER = new AtomicInteger(1);
    private final Integer id;

    // Ban public construction
    private ApplicationId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public static ApplicationId valueOf(Integer id) {
        return new ApplicationId(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ApplicationId)) {
            return false;
        }
        ApplicationId other = (ApplicationId) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Returns a new application id.
     *
     * @return app id
     */
    public static ApplicationId getAppId() {
        return new ApplicationId(ApplicationId.ID_DISPENCER.getAndIncrement());
    }

}
