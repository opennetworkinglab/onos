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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class YobBooleanTest {

    private static final String BOOLEAN_LIST = "booleanList";
    private static final String AUTO_PREFIX_BOOLEAN = "yangAutoPrefixBoolean";

    /*
        BOOLEAN
        Positive scenario
        input with in true and false
    */
    @Test
    public void positiveTest() {
        YangRequestWorkBench defaultYdtBuilder = YdtTestUtils.booleanYdt();
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

            Field field = yangObject.getClass().getDeclaredField(BOOLEAN_LIST);
            field.setAccessible(true);
            List booleanList = (List) field.get(yangObject);
            Field invalidInterval = booleanList.get(0).getClass()
                    .getDeclaredField(AUTO_PREFIX_BOOLEAN);
            invalidInterval.setAccessible(true);
            assertTrue((boolean) invalidInterval.get(booleanList.get(0)));
            assertFalse((boolean) invalidInterval.get(booleanList.get(1)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail();
        }
    }
}
