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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { RestService } from '../../../../app/fw/remote/rest.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';

class MockFnService {}

class MockUrlFnService {}

/**
 * ONOS GUI -- Remote -- REST Service - Unit Tests
 */
describe('RestService', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [RestService,
                { provide: FnService, useClass: MockFnService },
                { provide: LogService, useValue: log },
                { provide: UrlFnService, useClass: MockUrlFnService },
            ]
        });
    });

    it('should be created', inject([RestService], (service: RestService) => {
        expect(service).toBeTruthy();
    }));
});
