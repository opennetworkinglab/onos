/*
 * Copyright 2016 Open Networking Laboratory
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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import static org.apache.commons.io.FileUtils.contentEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.onosproject.yangutils.utils.io.impl.CopyrightHeader.getCopyrightHeader;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Unit Tests for the CopyrightHeader contents.
 */
public final class CopyrightHeaderTest {

    private final Logger log = getLogger(getClass());

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

        String baseDir = System.getProperty("basedir");
        String path = "/src/test/resources/CopyrightHeader.txt";

        String licenseHeader = getCopyrightHeader();
        File test = new File("target/TestCopyrightHeader.txt");

        FileWriter out = new FileWriter(test);
        out.write(licenseHeader);
        out.close();

        assertThat(true, is(contentEquals(test, new File(baseDir + path))));
    }
}