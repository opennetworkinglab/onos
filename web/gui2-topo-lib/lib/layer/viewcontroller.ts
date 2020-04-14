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
import { FnService, LogService, PrefsService } from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

export interface ViewControllerPrefs {
    visible: string;
}

/*
 ONOS GUI -- View Controller.
 A base class for view controllers to extend from
 */
export abstract class ViewControllerImpl {
    id: string;
    displayName: string = 'View';
    name: string;
    prefs: ViewControllerPrefs;
    visibility: string;

    constructor(
        protected fs: FnService,
        protected log: LogService,
        protected ps: PrefsService
    ) {
        this.log.debug('View Controller constructed');
    }

    initialize() {
        this.name = this.displayName.toLowerCase().replace(/ /g, '_');
        this.prefs = {
            visible: this.name + '_visible',
        };
    }

    enabled() {
        return this.ps.getPrefs('topo2_prefs', null)[this.prefs.visible];
    }

    isVisible() {
        return this.visibility;
    }

    hide() {
        this.visibility = 'hidden';
    }

    show() {
        this.visibility = 'visible';
    }

    toggle() {
        if (this.visibility === 'hidden') {
            this.visibility = 'visible';
        } else if (this.visibility === 'visible') {
            this.visibility = 'hidden';
        }
    }

    lookupPrefState(key: string): number {
        // Return 0 if not defined
        return this.ps.getPrefs('topo2_prefs', null)[key] || 0;
    }

    updatePrefState(key: string, value: number) {
        const state = this.ps.getPrefs('topo2_prefs', null);
        state[key] = value ? 1 : 0;
        this.ps.setPrefs('topo2_prefs', state);
    }
}
