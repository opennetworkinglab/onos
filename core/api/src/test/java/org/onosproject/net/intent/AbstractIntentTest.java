package org.onosproject.net.intent;

import org.junit.After;
import org.junit.Before;
import org.onosproject.core.IdGenerator;

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
