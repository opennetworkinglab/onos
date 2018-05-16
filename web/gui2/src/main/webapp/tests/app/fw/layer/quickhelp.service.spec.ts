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
import { QuickHelpService } from '../../../../app/fw/layer/quickhelp.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { LionService } from '../../../../app/fw/util/lion.service';
import { LoadingService } from '../../../../app/fw/layer/loading.service';
import { SvgUtilService } from '../../../../app/fw/svg/svgutil.service';

class MockFnService {}

class MockLionService {}

class MockLoadingService {}

class MockSvgUtilService {}

/**
 * ONOS GUI -- Layer -- Quick Help Service - Unit Tests
 */
describe('QuickHelpService', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [QuickHelpService,
                { provide: LogService, useValue: log },
                { provide: FnService, useClass: MockFnService },
                { provide: LionService, useClass: MockLionService },
                { provide: LoadingService, useClass: MockLoadingService },
                { provide: SvgUtilService, useClass: MockSvgUtilService },
            ]
        });
    });

    it('should be created', inject([QuickHelpService], (service: QuickHelpService) => {
        expect(service).toBeTruthy();
    }));
});
