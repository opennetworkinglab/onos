package org.onlab.onos.net.intent;

import org.onlab.onos.TestApplicationId;

/**
 * An intent used in the unit test.
 */
public class TestIntent extends Intent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestIntent(IntentId id) {
        super(id, new TestApplicationId("foo"), null);
    }

    /**
     * Constructor for serializer.
     */
    protected TestIntent() {
        super();
    }
}
