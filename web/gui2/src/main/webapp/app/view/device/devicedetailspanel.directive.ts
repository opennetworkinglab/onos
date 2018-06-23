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
import { Directive, Inject } from '@angular/core';
import { KeyService } from '../../fw/util/key.service';
import { LogService } from '../../log.service';

/**
 * ONOS GUI -- Device Details Panel Directive
 *
 * TODO: figure out if this should be a directive or a component. In the old
 * code it was a directive, but was referred to in device.html like a component
 * would be
 */
@Directive({
  selector: '[onosDeviceDetailsPanel]'
})
export class DeviceDetailsPanelDirective {

    constructor(
        private ks: KeyService,
        private log: LogService
    ) {
        this.log.debug('DeviceDetailsPanelDirective constructed');
    }

}
