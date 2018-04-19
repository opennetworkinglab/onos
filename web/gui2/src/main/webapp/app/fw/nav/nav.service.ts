/*
 * Copyright 2015-present Open Networking Foundation
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
import { FnService } from '../util/fn.service';
import { LogService } from '../../log.service';

/**
 * ONOS GUI -- Navigation Service
 */
@Injectable()
export class NavService {
    public showNav = false;

    constructor(
        private _fn_: FnService,
        private log: LogService
    ) {
        this.log.debug('NavService constructed');
    }

    hideNav() {
        this.showNav = !this.showNav;
        if (!this.showNav) {
            this.log.debug('Hiding Nav menu');
        }
    }

    toggleNav() {
        this.showNav = !this.showNav;
        if (this.showNav) {
            this.log.debug('Showing Nav menu');
        } else {
            this.log.debug('Hiding Nav menu');
        }
    }

}
