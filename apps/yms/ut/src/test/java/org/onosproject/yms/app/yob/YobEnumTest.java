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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class YobEnumTest {

/*
    ENUM

    Positive scenario

        input with in enum
        input with "ten"
        input with "hundred"
        input with "thousand"
*/

    @Test
    public void positiveTest() {
        YangRequestWorkBench defaultYdtBuilder = YdtTestUtils.enumYdt();
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
            Field field = yangObject.getClass().getDeclaredField("enumList");
            field.setAccessible(true);
            List enumList = (List) field.get(yangObject);
            assertEquals(false, enumList.isEmpty());
            Field enumleaf = enumList.get(0)
                    .getClass().getDeclaredField("enumleaf");
            enumleaf.setAccessible(true);
            assertEquals("ten", enumleaf
                    .get(enumList.get(0)).toString().toLowerCase());
            assertEquals("hundred", enumleaf
                    .get(enumList.get(1)).toString().toLowerCase());
            assertEquals("thousand", enumleaf
                    .get(enumList.get(2)).toString().toLowerCase());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail();
        }
    }
}
