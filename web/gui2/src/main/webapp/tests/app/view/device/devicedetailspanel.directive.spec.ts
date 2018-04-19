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
import { DeviceDetailsPanelDirective } from '../../../../app/view/device/devicedetailspanel.directive';
import { KeyService } from '../../../../app/fw/util/key.service';
import { LogService } from '../../../../app/log.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { ActivatedRoute, Router} from '@angular/router';

class MockFunctionService extends FnService {
    // Override things as necessary
}

class MockKeyService extends KeyService {
    // Override things as necessary
}
/**
 * ONOS GUI -- Device View Module - Unit Tests
 */
describe('DeviceDetailsPanelDirective', () => {
    let ar: ActivatedRoute;
    let log: LogService;
    let ks: KeyService;
    let fs: MockFunctionService;
    let window: Window
    let directive: DeviceDetailsPanelDirective;

    beforeEach(() => {
        log = new LogService();
        ar = new ActivatedRoute();
        fs = new MockFunctionService(ar, log);
        ks = new MockKeyService(fs, log);
        directive = new DeviceDetailsPanelDirective(ks, log, window);
    });

    afterEach(() => {
        fs = null;
        ks = null;
        log = null;
        ar = null;
        directive = null;
    });

    it('should create an instance', () => {
        expect(directive).toBeTruthy();
    });
});
