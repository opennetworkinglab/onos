/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.flow;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Unit tests for the BatchOperationTest object.
 */
public class BatchOperationTest {

    private enum TestType {
        OP1,
        OP2,
        OP3
    }

    final TestEntry entry1 = new TestEntry(TestType.OP1, new TestTarget(1));
    final TestEntry entry2 = new TestEntry(TestType.OP2, new TestTarget(2));


    private static final class TestTarget {
        private int id;

        private TestTarget(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null) {
                return false;
            }

            if (getClass() != o.getClass()) {
                return false;
            }
            TestTarget that = (TestTarget) o;
            return this.id == that.id;
        }

    }

    private static final class TestEntry extends BatchOperationEntry<TestType, TestTarget> {
        public TestEntry(TestType operator, TestTarget target) {
            super(operator, target);
        }
    }

    private static final class TestOperation extends BatchOperation<TestEntry> {
        private TestOperation() {
            super();
        }

        private TestOperation(Collection<TestEntry> batchOperations) {
            super(batchOperations);
        }
    }

    /**
     * Checks that the DefaultFlowRule class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(BatchOperation.class);
    }

    /**
     * Tests the equals(), hashCode() and toString() operations.
     */
    @Test
    public void testEquals() {
        final List<TestEntry> ops1 = new LinkedList<>();
        ops1.add(entry1);
        final List<TestEntry> ops2 = new LinkedList<>();
        ops2.add(entry2);

        final TestOperation op1 = new TestOperation(ops1);
        final TestOperation sameAsOp1 = new TestOperation(ops1);
        final TestOperation op2 = new TestOperation(ops2);

        new EqualsTester()
                .addEqualityGroup(op1, sameAsOp1)
                .addEqualityGroup(op2)
                .testEquals();
    }

    /**
     * Tests the constructors for a BatchOperation.
     */
    @Test
    public void testConstruction() {
        final List<TestEntry> ops = new LinkedList<>();
        ops.add(entry2);

        final TestOperation op1 = new TestOperation();
        assertThat(op1.size(), is(0));
        assertThat(op1.getOperations(), hasSize(0));

        final TestOperation op2 = new TestOperation(ops);
        op1.addOperation(entry1);
        op1.addAll(op2);
        assertThat(op1.size(), is(2));
        assertThat(op1.getOperations(), hasSize(2));

        op2.clear();
        assertThat(op2.size(), is(0));
        assertThat(op2.getOperations(), hasSize(0));
    }

    /**
     * Tests the constructor for BatchOperationEntries.
     */
    @Test
    public void testEntryConstruction() {
        final TestEntry entry = new TestEntry(TestType.OP3, new TestTarget(3));

        assertThat(entry.operator(), is(TestType.OP3));
        assertThat(entry.target().id, is(3));
    }
}
