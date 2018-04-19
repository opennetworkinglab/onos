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
import { FlashChangesDirective } from '../../../../app/fw/widget/flashchanges.directive';

import { LogService } from '../../../../app/log.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { ActivatedRoute, Router} from '@angular/router';

class MockFunctionService extends FnService {
    // Override things as necessary
}

/**
 * ONOS GUI -- Widget -- Table Flash Changes Directive - Unit Tests
 */
describe('FlashChangesDirective', () => {
    let fs: MockFunctionService;
    let log: LogService;
    let ar: ActivatedRoute;
    let directive: FlashChangesDirective;

    beforeEach(() => {
        log = new LogService();
        ar = new ActivatedRoute();
        fs = new MockFunctionService(ar, log);
        directive = new FlashChangesDirective(fs, log);
    });

    afterEach(() => {
        fs = null;
        log = null;
        ar = null;
        directive = null;
    });

    it('should create an instance', () => {
        expect(directive).toBeTruthy();
    });
});
