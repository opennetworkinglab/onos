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

package org.onosproject.yms.app.yob;

import org.junit.Assert;
import org.junit.Test;
import org.onosproject.yms.app.ydt.YdtTestUtils;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class YobDecimal64Test {

    /*

    Positive scenario

    input at boundary for decimal64 with fraction 2
        i. min value
        ii. max value

    input at boundary for decimal64 with minimum fraction
        i. min value
        ii. mid value
        iii. max value

    input at boundary for decimal64 with maximum fraction
        i. min value
        ii. mid value
        iii. max value

    input with in range
        if range is 10 to 100 for integer
            i.1. input 11
            i.2. min value 10
            i.3. max value 100

    input with multi interval range
        if range is 10..40 | 50..100 for decimal64
            i.1. input 11
            i.2. input 10
            i.3. input 40
            i.4. input 50
            i.5. input 55
            i.6. input 100

        if range is "min .. 3.14 | 10 | 20..max" for decimal64
            i.1. input min
            i.2. input 2.505
            i.3. input 3.14
            i.4. input 10
            i.5. input 20
            i.6. input 92233720368547757
            i.7. input 92233720368547758.07

    */
    @Test
    public void positiveTest() {
        YangRequestWorkBench defaultYdtBuilder = YdtTestUtils.decimal64Ydt();
        validateYangObject(defaultYdtBuilder);
    }

    private void validateYangObject(YangRequestWorkBench defaultYdtBuilder) {

        YdtContext rootCtx = defaultYdtBuilder.getRootNode();

        YdtContext childCtx = rootCtx.getFirstChild();

        DefaultYobBuilder builder = new DefaultYobBuilder();

        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, YdtTestUtils
                        .getSchemaRegistry());
        assertNotNull(yangObject);
        try {
            Field negInt = yangObject.getClass().getDeclaredField("negInt");
            negInt.setAccessible(true);
            assertEquals("-92233720368547758.08", negInt
                    .get(yangObject).toString());
            Field negIntWithMaxFraction = yangObject.getClass()
                    .getDeclaredField("negIntWithMaxFraction");
            negIntWithMaxFraction.setAccessible(true);
            assertEquals("-9.223372036854775808", negIntWithMaxFraction
                    .get(yangObject).toString());
            Field negIntWithMinFraction = yangObject.getClass()
                    .getDeclaredField("negIntWithMinFraction");
            negIntWithMinFraction.setAccessible(true);
            assertEquals("-922337203685477580.8", negIntWithMinFraction
                    .get(yangObject).toString());
            Field posInt = yangObject.getClass()
                    .getDeclaredField("posInt");
            posInt.setAccessible(true);
            assertEquals("92233720368547758.07", posInt
                    .get(yangObject).toString());
            Field posIntWithMaxFraction = yangObject
                    .getClass().getDeclaredField("posIntWithMaxFraction");
            posIntWithMaxFraction.setAccessible(true);
            assertEquals("9.223372036854775807", posIntWithMaxFraction
                    .get(yangObject).toString());
            Field posIntWithMinFraction = yangObject.getClass()
                    .getDeclaredField("posIntWithMinFraction");
            posIntWithMinFraction.setAccessible(true);
            assertEquals("922337203685477580.7", posIntWithMinFraction
                    .get(yangObject).toString());
            Field minIntWithRange = yangObject.getClass()
                    .getDeclaredField("minIntWithRange");
            minIntWithRange.setAccessible(true);
            assertEquals("10", minIntWithRange
                    .get(yangObject).toString());
            Field midIntWithRange = yangObject
                    .getClass().getDeclaredField("midIntWithRange");
            midIntWithRange.setAccessible(true);
            assertEquals("11", midIntWithRange.get(yangObject).toString());
            Field maxIntWithRange = yangObject
                    .getClass().getDeclaredField("maxIntWithRange");
            maxIntWithRange.setAccessible(true);
            assertEquals("100", maxIntWithRange.get(yangObject).toString());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail();
        }
    }
}
