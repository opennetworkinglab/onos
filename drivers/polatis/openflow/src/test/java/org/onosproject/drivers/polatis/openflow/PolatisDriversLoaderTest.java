/*
 * Copyright 2018 Open Networking Foundation
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
package org.onosproject.drivers.polatis.openflow;

import org.junit.Before;
import org.onosproject.net.driver.AbstractDriverLoaderTest;
import org.onosproject.ui.UiExtensionServiceAdapter;

/**
 * Polatis OpenFlow drivers loader test.
 */
public class PolatisDriversLoaderTest extends AbstractDriverLoaderTest {

    @Before
    public void setUp() {
        loader = new PolatisDriversLoader();
        ((PolatisDriversLoader) loader).uiExtensionService = new UiExtensionServiceAdapter();
    }
}
