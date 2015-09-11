/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.optical.cfg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public class corresponding to JSON described data model.
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
public class OpticalNetworkConfig {
    protected static final Logger log = LoggerFactory.getLogger(OpticalNetworkConfig.class);

    private List<OpticalSwitchDescription> opticalSwitches;
    private List<OpticalLinkDescription> opticalLinks;

    public OpticalNetworkConfig() {
        opticalSwitches = new ArrayList<>();
        opticalLinks = new ArrayList<>();
    }

    public List<OpticalSwitchDescription> getOpticalSwitches() {
        return opticalSwitches;
    }

    public void setOpticalSwitches(List<OpticalSwitchDescription> switches) {
        this.opticalSwitches = switches;
    }

    public List<OpticalLinkDescription> getOpticalLinks() {
        return opticalLinks;
    }

    public void setOpticalLinks(List<OpticalLinkDescription> links) {
        this.opticalLinks = links;
    }

}

