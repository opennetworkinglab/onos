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

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
     * @throws SecurityException if any security violation is observed.
     * @throws NoSuchMethodException if when the method is not found.
     * @throws IllegalArgumentException if there is illegal argument found.
     * @throws InstantiationException if instantiation is provoked for the private constructor.
     * @throws IllegalAccessException if instance is provoked or a method is provoked.
     * @throws InvocationTargetException when an exception occurs by the method or constructor.
     */
    @Test
    public void callPrivateConstructors() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
    InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?>[] classesToConstruct = {CopyrightHeader.class };
        for (Class<?> clazz : classesToConstruct) {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance());
        }
    }

    /**
     * This test case checks the received copyright header contents.
     */
    @Test
    public void testGetCopyrightHeader() throws IOException {

        CopyrightHeader.parseCopyrightHeader();
        String licenseHeader = CopyrightHeader.getCopyrightHeader();
        ClassLoader classLoader = CopyrightHeaderTest.class.getClassLoader();
        File test = new File("target/TestCopyrightHeader.txt");

        FileWriter out = new FileWriter(test);
        out.write(licenseHeader);
        out.close();

        File temp = new File("target/temp.txt");
        InputStream stream = classLoader.getResourceAsStream("CopyrightHeader.txt");
        OutputStream outStream = new FileOutputStream(temp);
        int i;
        while ((i = stream.read()) != -1) {
            outStream.write(i);
        }
        outStream.close();
        stream.close();

        BufferedReader br1 = new BufferedReader(new FileReader(test));
        BufferedReader br2 = new BufferedReader(new FileReader(temp));
        while (br1.readLine() != null && br2.readLine() != null) {
            assertThat(true, is((br1.readLine()).equals(br2.readLine())));
        }
        br1.close();
        br2.close();
    }
}