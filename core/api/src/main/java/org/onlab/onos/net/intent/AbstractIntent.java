package org.onlab.onos.net.intent;

/**
 * Base intent implementation.
 */
public abstract class AbstractIntent implements Intent {

    private final IntentId id;

    /**
     * Creates a base intent with the specified identifier.
     *
     * @param id intent identifier
     */
    protected AbstractIntent(IntentId id) {
        this.id = id;
    }

    /**
     * Constructor for serializer.
     */
    protected AbstractIntent() {
        this.id = null;
    }

    @Override
    public IntentId id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractIntent that = (AbstractIntent) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
