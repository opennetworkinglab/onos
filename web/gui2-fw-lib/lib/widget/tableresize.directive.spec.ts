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
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import { TableResizeDirective } from './tableresize.directive';
import { LogService } from '..//log.service';
import { ConsoleLoggerService } from '../consolelogger.service';
import { MastService } from '../mast/mast.service';
import { FnService } from '../util/fn.service';

class MockMastService {}

class MockFnService extends FnService {
    constructor(ar: ActivatedRoute, log: LogService, w: Window) {
        super(ar, log, w);
    }
}

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Widget -- Table Resize Directive - Unit Tests
 */
describe('TableResizeDirective', () => {
    let log: LogService;
    let mockWindow: Window;
    let ar: ActivatedRoute;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute(['debug', 'DetectBrowserDirective']);
        mockWindow = <any>{
            navigator: {
                userAgent: 'HeadlessChrome',
                vendor: 'Google Inc.'
            }
        };
        TestBed.configureTestingModule({
            providers: [ TableResizeDirective,
                { provide: FnService, useValue: new MockFnService(ar, log, mockWindow) },
                { provide: LogService, useValue: log },
                { provide: MastService, useClass: MockMastService },
                { provide: 'Window', useFactory: (() => mockWindow ) },
            ]
        });
    });

    afterEach(() => {
        log = null;
    });

    it('should create an instance', inject([TableResizeDirective], (directive: TableResizeDirective) => {
        expect(directive).toBeTruthy();
    }));
});
