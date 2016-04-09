/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages the user interface topology layouts.
 * Note that these layouts are persisted and distributed across the cluster.
 */
@Component(immediate = true)
@Service
public class UiTopoLayoutManager implements UiTopoLayoutService {

//    private static final ClassLoader CL =
//            UiTopoLayoutManager.class.getClassLoader();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    public void activate() {
        // TODO: implement starting stuff
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // TODO: implement stopping stuff
        log.info("Stopped");
    }


    @Override
    public List<UiTopoLayout> getLayouts() {
        // TODO: implement
        return null;
    }

    @Override
    public boolean addLayout(UiTopoLayout layout) {
        // TODO: implement
        return false;
    }

    @Override
    public boolean removeLayout(UiTopoLayout layout) {
        // TODO: implement
        return false;
    }
}
