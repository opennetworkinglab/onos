/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.onosproject.drivers.microsemi.yang.impl.MseaSaFilteringManager;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFilteringService;
import org.onosproject.yms.app.yab.MockYmsManager;

public class MockMseaSaFilteringManager extends MseaSaFilteringManager {

    @Override
    public void activate() {
        try {
            ymsService = new MockYmsManager();
            ych = ymsService.getYangCodecHandler();
            ych.addDeviceSchema(MseaSaFilteringService.class);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Failed to load YMS Manager: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Failed to load YMS Manager: " + e.getMessage());
            throw new UncheckedIOException(e);
        }
    }
}
