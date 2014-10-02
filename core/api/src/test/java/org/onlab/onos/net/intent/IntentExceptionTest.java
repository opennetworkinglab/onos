package org.onlab.onos.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test of the intent exception.
 */
public class IntentExceptionTest {

    @Test
    public void basics() {
        validate(new IntentException(), null, null);
        validate(new IntentException("foo"), "foo", null);

        Throwable cause = new NullPointerException("bar");
        validate(new IntentException("foo", cause), "foo", cause);
    }

    /**
     * Validates that the specified exception has the correct message and cause.
     *
     * @param e       exception to test
     * @param message expected message
     * @param cause   expected cause
     */
    protected void validate(RuntimeException e, String message, Throwable cause) {
        assertEquals("incorrect message", message, e.getMessage());
        assertEquals("incorrect cause", cause, e.getCause());
    }

}
