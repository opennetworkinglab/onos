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

package org.onosproject.drivers.polatis.snmp;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.net.driver.AbstractDriverLoader;
import org.onosproject.net.optical.OpticalDevice;
import org.onosproject.ui.UiGlyph;
import org.onosproject.ui.UiGlyphFactory;
import org.onosproject.ui.UiExtensionService;

import com.google.common.collect.ImmutableList;

/**
 * Loader for Polatis Snmp device drivers.
 */
@Component(immediate = true)
public class PolatisDriversLoader extends AbstractDriverLoader {

    // OSGI: help bundle plugin discover runtime package dependency.
    @SuppressWarnings("unused")
    private OpticalDevice optical;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    private UiGlyphFactory glyphFactory =
        () -> ImmutableList.of(
            new UiGlyph("policon", "0 0 64 64",
                "M 32.024746,2 30.163615,19.069136 24.258784,3.015638 "
                + "26.879599,19.985033 17.021343,6.007051 23.943688,21.71947 "
                + "10.8045,10.769161 21.557349,24.15439 6.031794,16.978659 "
                + "19.883076,27.1245 3.027943,24.21114 19.033986,30.42674 "
                + "2,31.97526 19.069136,33.83639 3.015638,39.74122 "
                + "19.985033,37.12041 6.007051,46.97866 21.719466,40.05632 "
                + "10.769161,53.19551 24.154391,42.44265 16.978659,57.96822 "
                + "27.124504,44.11693 24.21114,60.97206 30.426738,44.96602 "
                + "31.975259,62 33.83639,44.93086 39.74122,60.98437 "
                + "37.120405,44.01497 46.978663,57.99296 40.056317,42.28054 "
                + "53.195507,53.23084 42.442656,39.84561 57.968215,47.02135 "
                + "44.116927,36.8755 60.972063,39.78886 44.966018,33.57327 "
                + "62,32.02475 44.930865,30.16362 60.984369,24.25878 "
                + "44.014972,26.8796 57.992959,17.021342 42.280539,23.94369 "
                + "53.23084,10.8045 39.845614,21.55735 47.021349,6.031794 "
                + "36.875501,19.883076 39.788865,3.027943 33.573267,19.033986 Z "
                + "m -0.05497,19.23081 A 10.768943,10.768943 0 0 1 "
                + "42.769201,31.96977 10.768943,10.768943 0 0 1 "
                + "32.030235,42.7692 10.768943,10.768943 0 0 1 "
                + "21.230812,32.03023 10.768943,10.768943 0 0 1 "
                + "31.969778,21.23081 Z")
            );

    public PolatisDriversLoader() {
        super("/polatis-snmp-drivers.xml");
    }

    @Activate
    @Override
    protected void activate() {
        uiExtensionService.register(glyphFactory);
        super.activate();
    }

    @Deactivate
    @Override
    protected void deactivate() {
        uiExtensionService.unregister(glyphFactory);
        super.deactivate();
    }

}
