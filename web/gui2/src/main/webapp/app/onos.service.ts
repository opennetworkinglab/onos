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
import {Inject, Injectable} from '@angular/core';
import { LogService } from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

/**
 * A structure of View elements for the OnosService
 */
export interface View {
    id: string;
    path: string;
}

/**
 * ONOS GUI -- OnosService - a placeholder for the global onos variable
 */
@Injectable({
  providedIn: 'root',
})
export class OnosService {
    // Global variable
    public browser: string;
    public mobile: boolean;
    public viewMap: View[];
    public username: string;

    constructor (
        private log: LogService,
        @Inject('Window') private window: any
    ) {
        // The onosUser is added to the index.html by MainIndexResource
        this.username = this.window['onosUser'];
        this.log.debug('OnosService constructed');
    }
}
