/**
 * Copyright 2016-present Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.converter;

import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;

import java.util.HashMap;
import java.util.Map;

import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.DEVICE;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;

/**
 * Returns the appropriate converter object based on the ONOS event type.
 *
 */
public final class ConversionFactory {

    // Store converters for all supported events
    private Map<Type, EventConverter> converters =
            new HashMap<Type, EventConverter>() {
                {
                    put(DEVICE, new DeviceEventConverter());
                    put(LINK, new LinkEventConverter());
                }
            };

    // Exists to defeat instantiation
    private ConversionFactory() {
    }

    private static class SingletonHolder {
        private static final ConversionFactory INSTANCE =
                new ConversionFactory();
    }

    /**
     * Returns a static reference to the Conversion Factory.
     *
     * @return singleton object
     */
    public static ConversionFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Returns an Event converter object for the specified ONOS event type.
     *
     * @param event ONOS event type
     * @return Event Converter object
     */
    public EventConverter getConverter(Type event) {
        return converters.get(event);
    }

}
