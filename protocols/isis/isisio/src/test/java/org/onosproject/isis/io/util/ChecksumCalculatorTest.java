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
package org.onosproject.isis.io.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for ChecksumCalculator.
 */
public class ChecksumCalculatorTest {

    private final byte[] l1Lsp = {
            -125, 27, 1, 0, 18, 1, 0, 0, 0, 86, 4, -81, 34, 34, 34,
            34, 34, 34, 0, 0, 0, 0, 0, 9, 99, 11, 1, 1, 4, 3, 73,
            0, 10, -127, 1, -52, -119, 2, 82, 50, -124, 4, -64, -88, 10, 1, -128,
            24, 10, -128, -128, -128, 10, 0, 10, 0, -1, -1, -1, -4, 10, -128, -128,
            -128, -64, -88, 10, 0, -1, -1, -1, 0, 2, 12, 0, 10, -128, -128, -128,
            51, 51, 51, 51, 51, 51, 2
    };
    private ChecksumCalculator calculator;
    private byte[] result;
    private boolean result1;

    @Before
    public void setUp() throws Exception {
        calculator = new ChecksumCalculator();
    }

    @After
    public void tearDown() throws Exception {
        calculator = null;
    }

    /**
     * Tests validateLspCheckSum() method.
     */
    @Test
    public void testValidateLspCheckSum() throws Exception {
        result1 = calculator.validateLspCheckSum(l1Lsp, IsisConstants.CHECKSUMPOSITION,
                                                 IsisConstants.CHECKSUMPOSITION + 1);

        assertThat(result1, is(true));
    }

    /**
     * Tests calculateLspChecksum() method.
     */
    @Test
    public void testCalculateLspChecksum() throws Exception {
        result = calculator.calculateLspChecksum(l1Lsp, IsisConstants.CHECKSUMPOSITION,
                                                 IsisConstants.CHECKSUMPOSITION + 1);
        assertThat(result[0],
                   is(l1Lsp[IsisConstants.CHECKSUMPOSITION]));
        assertThat(result[1],
                   is(l1Lsp[IsisConstants.CHECKSUMPOSITION + 1]));
    }
}