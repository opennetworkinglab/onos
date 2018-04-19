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
import { TableResizeDirective } from '../../../../app/fw/widget/tableresize.directive';
import { LogService } from '../../../../app/log.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { MastService } from '../../../../app/fw/mast/mast.service';
import { ActivatedRoute, Router} from '@angular/router';

class MockFunctionService extends FnService {
    // Override things as necessary
}

/**
 * ONOS GUI -- Widget -- Table Resize Directive - Unit Tests
 */
describe('TableResizeDirective', () => {

    let fs: MockFunctionService;
    let log: LogService;
    let ar: ActivatedRoute;
    let ms: MastService;
    let directive: TableResizeDirective;

    beforeEach(() => {
        log = new LogService();
        ar = new ActivatedRoute();
        fs = new MockFunctionService(ar, log);
        ms = new MastService(fs, log);
        directive = new TableResizeDirective(fs, log, ms);
    });

    afterEach(() => {
        fs = null;
        log = null;
        ar = null;
        ms = null;
        directive = null;
    });

    it('should create an instance', () => {
        expect(directive).toBeTruthy();
    });
});
