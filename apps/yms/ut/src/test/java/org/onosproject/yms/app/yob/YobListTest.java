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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class YobListTest {

    @Test
    public void listwithoutcontainerTest() {
        YangRequestWorkBench defaultYdtBuilder =
                YdtTestUtils.listWithoutContainerYdt();
        validateYangObjectList(defaultYdtBuilder);
    }

    private void validateYangObjectList(
            YangRequestWorkBench defaultYdtBuilder) {

        YdtContext rootCtx = defaultYdtBuilder.getRootNode();

        YdtContext childCtx = rootCtx.getFirstChild();

        DefaultYobBuilder builder = new DefaultYobBuilder();

        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, YdtTestUtils.
                        getSchemaRegistry());
        assertNotNull(yangObject);
        assertTrue("RootlistOpParam".equals(yangObject.getClass().getSimpleName()));
        try {

            Field field =
                    yangObject.getClass().getDeclaredField("listwithcontainer");
            field.setAccessible(true);
            List listwithcontainer = (List) field.get(yangObject);
            assertNull(listwithcontainer);
            Field field1 = yangObject.getClass()
                    .getDeclaredField("listwithoutcontainer");
            field1.setAccessible(true);
            List listwithoutcontainer = (List) field1.get(yangObject);
            assertEquals(false, listwithoutcontainer.isEmpty());
            Field invalidinterval = listwithoutcontainer.get(0).getClass()
                    .getDeclaredField("invalidinterval");
            invalidinterval.setAccessible(true);
            assertEquals("12", invalidinterval.get(listwithoutcontainer.get(0))
                    .toString());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail();
        }
    }

    @Test
    public void listwithcontainerTest()
            throws IOException {
        YangRequestWorkBench defaultYdtBuilder =
                YdtTestUtils.listWithContainerYdt();

        validateYangObject(defaultYdtBuilder);
    }

    public void validateYangObject(YangRequestWorkBench defaultYdtBuilder) {

        YdtContext ydtContext = defaultYdtBuilder.getRootNode();

        YdtContext ydtContext1 = ydtContext.getFirstChild();

        DefaultYobBuilder defaultYobBuilder = new DefaultYobBuilder();

        Object yangObject = defaultYobBuilder.getYangObject(
                (YdtExtendedContext) ydtContext1, YdtTestUtils
                        .getSchemaRegistry());
        assertNotNull(yangObject);
        assertTrue("RootlistOpParam".equals(yangObject.getClass().getSimpleName()));
        try {

            Field field = yangObject.getClass()
                    .getDeclaredField("listwithoutcontainer");
            field.setAccessible(true);
            List listwithoutcontainer = (List) field.get(yangObject);
            assertNull(listwithoutcontainer);
            Field listwithcontainerField =
                    yangObject.getClass().getDeclaredField("listwithcontainer");
            listwithcontainerField.setAccessible(true);
            List listwithcontainer =
                    (List) listwithcontainerField.get(yangObject);
            Field invalid = listwithcontainer.get(0).getClass()
                    .getDeclaredField("invalid");
            invalid.setAccessible(true);
            assertEquals("12",
                         invalid.get(listwithcontainer.get(0)).toString());
            Field invalidinterval = listwithcontainer.get(0).getClass()
                    .getDeclaredField("invalidinterval");
            invalidinterval.setAccessible(true);
            List invalidintervalList =
                    (List) invalidinterval.get(listwithcontainer.get(0));
            assertEquals("1", invalidintervalList.get(0).toString());
            assertEquals("2", invalidintervalList.get(1).toString());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail();
        }
    }
}
