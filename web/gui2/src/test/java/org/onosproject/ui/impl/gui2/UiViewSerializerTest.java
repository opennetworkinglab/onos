/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.ui.impl.gui2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UiViewSerializerTest {

    UiView uiView1 = new UiView(UiView.Category.NETWORK, "uiView1", "uiView1", "icon1");
    UiView uiView2 = new UiView(UiView.Category.OTHER, "uiView2", "uiView2", "icon2");

    @Before
    public void init() {
    }

    @Test
    public void uiViewSerializerTest() throws JsonProcessingException {
        UiViewSerializer serializer = new UiViewSerializer(UiView.class);
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module =
                new SimpleModule("UiViewSerializer");
        module.addSerializer(serializer);
        mapper.registerModule(module);
        String ext = mapper.writeValueAsString(uiView1);
        assertEquals("{\"id\":\"uiView1\",\"icon\":\"icon1\",\"cat\":\"NETWORK\",\"label\":\"uiView1\"}", ext);
    }
}
