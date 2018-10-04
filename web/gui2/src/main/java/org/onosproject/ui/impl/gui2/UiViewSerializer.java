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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiView;

import java.io.IOException;

public class UiViewSerializer extends StdSerializer<UiView> {

    public UiViewSerializer(Class<UiView> t) {
        super(t);
    }

    @Override
    public void serialize(UiView view,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", view.id());
        jsonGenerator.writeStringField("icon", view.iconId());
        jsonGenerator.writeStringField("cat", view.category().toString());
        jsonGenerator.writeStringField("label", view.label());
        jsonGenerator.writeEndObject();
    }
}
