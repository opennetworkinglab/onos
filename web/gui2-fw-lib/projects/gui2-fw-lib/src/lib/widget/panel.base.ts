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
import { FnService } from '../util/fn.service';
import { LogService } from '../log.service';


/**
 * Base model of panel view - implemented by Panel components
 */
export interface PanelBase {
    showPanel(cb: any): void;
    hidePanel(cb: any): void;
    togglePanel(cb: any): void;
    panelIsVisible(): boolean;
}

/**
 * ONOS GUI -- Widget -- Panel Base class
 *
 * Replacing the panel service in the old implementation
 */
export abstract class PanelBaseImpl implements PanelBase {

    on: boolean;

    protected constructor(
        protected fs: FnService,
        protected log: LogService
    ) {
//        this.log.debug('Panel base class constructed');
    }

    showPanel(cb) {
        this.on = true;
    }

    hidePanel(cb) {
        this.on = false;
    }

    togglePanel(cb): boolean {
        if (this.on) {
            this.hidePanel(cb);
        } else {
            this.showPanel(cb);
        }
        return this.on;
    }

    panelIsVisible(): boolean {
        return this.on;
    }

    /**
     * A dummy implementation of the lionFn until the response is received and the LION
     * bundle is received from the WebSocket
     */
    dummyLion(key: string): string {
        return '%' + key + '%';
    }
}
