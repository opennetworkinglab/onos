package org.onlab.onos.net.intent;
//TODO is this the right package?

/**
 * An intent used in the unit test.
 *
 * FIXME: we don't want to expose this class publicly, but the current Kryo
 * serialization mechanism does not allow this class to be private and placed
 * on testing directory.
 */
public class TestSubclassInstallableIntent extends TestInstallableIntent implements InstallableIntent {
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
