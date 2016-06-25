/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yangutils.utils.io.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.io.FileUtils.contentEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.utils.io.impl.CopyrightHeader.getCopyrightHeader;

/**
 * Unit Tests for the CopyrightHeader contents.
 */
public final class CopyrightHeaderTest {

    private static final String COPYRIGHTS_FIRST_LINE = "/*\n * Copyright " + Calendar.getInstance().get(Calendar.YEAR)
            + "-present Open Networking Laboratory\n";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Unit test for testing private constructor.
     *
     * @throws SecurityException if any security violation is observed
     * @throws NoSuchMethodException if when the method is not found
     * @throws IllegalArgumentException if there is illegal argument found
     * @throws InstantiationException if instantiation is provoked for the private constructor
     * @throws IllegalAccessException if instance is provoked or a method is provoked
     * @throws InvocationTargetException when an exception occurs by the method or constructor
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {CopyrightHeader.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThat(null, not(constructor.newInstance()));
        }
    }

    /**
     * This test case checks the received copyright header contents.
     *
     * @throws IOException when fails to do IO operations
     */
    @Test
    public void testGetCopyrightHeader() throws IOException {

        String path = "src/test/resources/CopyrightHeader.txt";

        File testRsc = new File(path);
        FileInputStream in = new FileInputStream(testRsc);

        File testFile = new File("target/TestHeader.txt");
        FileOutputStream out = new FileOutputStream(testFile);

        out.write(COPYRIGHTS_FIRST_LINE.getBytes());
        int c = 0;
        while ((c = in.read()) != -1) {
            out.write(c);
        }

        String licenseHeader = getCopyrightHeader();
        File test = new File("target/TestCopyrightHeader.txt");

        FileWriter writer = new FileWriter(test);
        writer.write(licenseHeader);
        writer.close();
        out.close();
        out.flush();
        in.close();

        assertThat(true, is(contentEquals(test, testFile)));
        test.delete();
        testFile.delete();
    }
}