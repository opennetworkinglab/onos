package org.onosproject.event;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Change}.
 */
public class ChangeTest {

    @Test
    public void getters() {
        Change<String> change = new Change<>("a", "b");
        assertEquals("a", change.oldValue());
        assertEquals("b", change.newValue());
    }

    @Test
    public void equality() {
        new EqualsTester()
        .addEqualityGroup(new Change<>("foo", "bar"),
                          new Change<>("foo", "bar"))
        .addEqualityGroup(new Change<>("bar", "car"))
        .testEquals();
    }
}
