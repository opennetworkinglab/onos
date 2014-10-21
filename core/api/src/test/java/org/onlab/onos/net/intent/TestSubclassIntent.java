package org.onlab.onos.net.intent;

/**
 * An intent used in the unit test.
 */
public class TestSubclassIntent extends TestIntent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestSubclassIntent(IntentId id) {
        super(id);
    }

    /**
     * Constructor for serializer.
     */
    protected TestSubclassIntent() {
        super();
    }
}
