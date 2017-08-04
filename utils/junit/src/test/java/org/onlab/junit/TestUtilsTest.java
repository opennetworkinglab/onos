/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.junit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils.TestUtilsException;

/**
 * Test and usage examples for TestUtils.
 */
public class TestUtilsTest {

    /**
     * Test data.
     */
    private static final class TestClass {

        @SuppressWarnings("unused")
        private int privateField = 42;

        @SuppressWarnings("unused")
        protected int protectedField = 2501; // CHECKSTYLE IGNORE THIS LINE

        /**
         * Protected method with multiple argument.
         *
         * @param x simply returns
         * @param y not used
         * @return x
         */
        @SuppressWarnings("unused")
        private int privateMethod(Number x, Long y) {
            return x.intValue();
        }

        /**
         * Protected method with no argument.
         *
         * @return int
         */
        @SuppressWarnings("unused")
        protected int protectedMethod() {
            return 42;
        }

        /**
         * Method returning array.
         *
         * @param ary random array
         * @return ary
         */
        @SuppressWarnings("unused")
        private int[] arrayReturnMethod(int[] ary) {
            return ary;
        }

        /**
         * Method without return value.
         *
         * @param s ignored
         */
        @SuppressWarnings("unused")
        private void voidMethod(String s) {
            System.out.println(s);
        }
    }

    private TestClass test;

    /**
     * Sets up the test fixture.
     */
    @Before
    public void setUp() {
        test = new TestClass();
    }

    /**
     * Example to access private field.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testSetGetPrivateField() throws TestUtilsException {

        assertEquals(42, (int) TestUtils.getField(test, "privateField"));
        TestUtils.setField(test, "privateField", 0xDEAD);
        assertEquals(0xDEAD, (int) TestUtils.getField(test, "privateField"));
    }

    /**
     * Example to access protected field.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testSetGetProtectedField() throws TestUtilsException {

        assertEquals(2501, (int) TestUtils.getField(test, "protectedField"));
        TestUtils.setField(test, "protectedField", 0xBEEF);
        assertEquals(0xBEEF, (int) TestUtils.getField(test, "protectedField"));
    }

    /**
     * Example to call private method and multiple parameters.
     * <p>
     * It also illustrates that paramTypes must match declared type,
     * not the runtime types of arguments.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testCallPrivateMethod() throws TestUtilsException {

        int result = TestUtils.callMethod(test, "privateMethod",
                        new Class<?>[] {Number.class, Long.class},
                        Long.valueOf(42), Long.valueOf(32));
        assertEquals(42, result);
    }

    /**
     * Example to call protected method and no parameters.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testCallProtectedMethod() throws TestUtilsException {

        int result = TestUtils.callMethod(test, "protectedMethod",
                        new Class<?>[] {});
        assertEquals(42, result);
    }

    /**
     * Example to call method returning array.
     * <p>
     * Note: It is not required to receive as Object.
     * Following is just verifying it is not Boxed arrays.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testCallArrayReturnMethod() throws TestUtilsException {

        int[] array = {1, 2, 3};
        Object aryResult = TestUtils.callMethod(test, "arrayReturnMethod",
                new Class<?>[] {int[].class}, array);
        assertEquals(int[].class, aryResult.getClass());
        assertArrayEquals(array, (int[]) aryResult);
    }


    /**
     * Example to call void returning method.
     * <p>
     * Note: Return value will be null for void methods.
     *
     * @throws TestUtilsException TestUtils error
     */
    @Test
    public void testCallVoidReturnMethod() throws TestUtilsException {

        Object voidResult = TestUtils.callMethod(test, "voidMethod",
                String.class, "foobar");
        assertNull(voidResult);
    }
}
