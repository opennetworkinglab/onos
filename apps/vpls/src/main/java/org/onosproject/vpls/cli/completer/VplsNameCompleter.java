/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vpls.cli.completer;

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.vpls.api.Vpls;
import org.onosproject.vpls.api.VplsData;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * VPLS name completer.
 */
public class VplsNameCompleter extends AbstractChoicesCompleter {

    protected static Vpls vpls;

    @Override
    public List<String> choices() {
        if (vpls == null) {
            vpls = get(Vpls.class);
        }
        Collection<VplsData> vplses = vpls.getAllVpls();
        return vplses.stream().map(VplsData::name).collect(Collectors.toList());
    }
}
