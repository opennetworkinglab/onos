package org.onlab.onos.net.intent;

import org.onlab.onos.TestApplicationId;

/**
 * An installable intent used in the unit test.
 */
public class TestInstallableIntent extends Intent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestInstallableIntent(IntentId id) {
        super(id, new TestApplicationId("foo"), null);
    }

    /**
     * Constructor for serializer.
     */
    protected TestInstallableIntent() {
        super();
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

}
