package net.onrc.onos.api;

/**
 * Notion of provider identity.
 */
public class ProviderId {

    private final String id;

    public ProviderId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProviderId that = (ProviderId) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ProviderId{" +
                "id='" + id + '\'' +
                '}';
    }
}
