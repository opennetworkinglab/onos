/*
 * Copyright 2014-present Open Networking Foundation
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
import { FnService } from '../../../../app/fw/util/fn.service';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Util -- General Purpose Functions - Unit Tests
 */
describe('FnService', () => {
    let log: LogService;
    let ar: ActivatedRoute;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute({'debug': 'TestService'});

        TestBed.configureTestingModule({
            providers: [FnService,
                { provide: LogService, useValue: log },
                { provide: ActivatedRoute, useValue: ar },
            ]
        });
    });

    it('should be created', inject([FnService], (service: FnService) => {
        expect(service).toBeTruthy();
    }));
});
