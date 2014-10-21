package org.onlab.onos.net.intent;

/**
 * An intent used in the unit test.
 */
public class TestSubclassInstallableIntent extends TestInstallableIntent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestSubclassInstallableIntent(IntentId id) {
        super(id);
    }

    /**
     * Constructor for serializer.
     */
    protected TestSubclassInstallableIntent() {
        super();
    }
}
