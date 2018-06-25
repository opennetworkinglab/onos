/*
 * Copyright 2017-present Open Networking Foundation
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
import { DialogService } from '../../../../app/fw/layer/dialog.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { KeyService } from '../../../../app/fw/util/key.service';

class MockFnService {}

class MockKeyService {}

/**
 * ONOS GUI -- Layer -- Dialog Service - Unit Tests
 */
describe('DialogService', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [DialogService,
                { provide: LogService, useValue: log },
                { provide: FnService, useClass: MockFnService },
                { provide: KeyService, useClass: MockKeyService },
            ]
        });
    });

    it('should be created', inject([DialogService], (service: DialogService) => {
        expect(service).toBeTruthy();
    }));
});
