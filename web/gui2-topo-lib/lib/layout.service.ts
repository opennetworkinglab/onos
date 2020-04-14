/*
 * Copyright 2019-present Open Networking Foundation
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
import { Injectable } from '@angular/core';
import {LogService, WebSocketService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

export enum LayoutType {
    LAYOUT_DEFAULT = 'default',
    LAYOUT_ACCESS = 'access'
}

/**
 * ONOS GUI - Layout service - connects to the Layout UI Extension app
 */
@Injectable()
export class LayoutService {

    constructor(
        protected log: LogService,
        protected wss: WebSocketService
    ) {
        this.log.debug('LayoutService constructed');
    }

    /**
     * tell the server we want a new layout
     * @param type The type of layout we want
     */
    changeLayout(type: LayoutType): void {
        this.wss.sendEvent('doLayout', {
            type: type
        });
        this.log.debug('Layout changed to', type);
    }
}
