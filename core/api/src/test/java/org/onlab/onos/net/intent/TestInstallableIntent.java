package org.onlab.onos.net.intent;
//TODO is this the right package?

import org.onlab.onos.net.Link;

import java.util.Collection;

/**
 * An installable intent used in the unit test.
 *
 * FIXME: we don't want to expose this class publicly, but the current Kryo
 * serialization mechanism does not allow this class to be private and placed
 * on testing directory.
 */
public class TestInstallableIntent extends AbstractIntent implements InstallableIntent {
    /**
     * Constructs an instance with the specified intent ID.
     *
     * @param id intent ID
     */
    public TestInstallableIntent(IntentId id) {
        super(id);
    }

    /**
     * Constructor for serializer.
     */
    protected TestInstallableIntent() {
        super();
    }

    @Override
    public Collection<Link> requiredLinks() {
        return null;
    }
}
