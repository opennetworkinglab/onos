package org.onlab.onos.net.intent;
//TODO is this the right package?

/**
 * An intent used in the unit test.
 *
 * FIXME: we don't want to expose this class publicly, but the current Kryo
 * serialization mechanism does not allow this class to be private and placed
 * on testing directory.
 */
public class TestIntent extends AbstractIntent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestIntent(IntentId id) {
        super(id);
    }

    /**
     * Constructor for serializer.
     */
    protected TestIntent() {
        super();
    }
}
