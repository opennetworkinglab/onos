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
import { TestBed, inject } from '@angular/core/testing';

import { LogService } from '../../app/log.service';
import { ConsoleLoggerService } from '../../app/consolelogger.service';
import { DetectBrowserDirective } from '../../app/detectbrowser.directive';
import { ActivatedRoute, Params } from '@angular/router';
import { FnService } from '../../app/fw/util/fn.service';
import { OnosService } from '../../app/onos.service';
import { of } from 'rxjs';

class MockFnService extends FnService {
    constructor(ar: ActivatedRoute, log: LogService) {
        super(ar, log);
    }
}

class MockOnosService {}

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Detect Browser Directive - Unit Tests
 */
describe('DetectBrowserDirective', () => {
    let log: LogService;
    let ar: ActivatedRoute;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute(['debug', 'DetectBrowserDirective']);

        TestBed.configureTestingModule({
            providers: [ DetectBrowserDirective,
                { provide: FnService, useValue: new MockFnService(ar, log) },
                { provide: LogService, useValue: log },
                { provide: OnosService, useClass: MockOnosService },
                { provide: Document, useValue: document },
            ]
        });
    });

    afterEach(() => {
        log = null;
    });

    it('should create an instance', inject([DetectBrowserDirective], (directive: DetectBrowserDirective) => {
        expect(directive).toBeTruthy();
    }));
});
