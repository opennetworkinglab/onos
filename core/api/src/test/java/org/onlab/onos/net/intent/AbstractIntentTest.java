package org.onlab.onos.net.intent;

import org.junit.After;
import org.junit.Before;
import org.onlab.onos.core.IdGenerator;

public abstract class AbstractIntentTest {

    protected IdGenerator idGenerator = new MockIdGenerator();

    @Before
    public void setUp() throws Exception {
        Intent.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }
}
