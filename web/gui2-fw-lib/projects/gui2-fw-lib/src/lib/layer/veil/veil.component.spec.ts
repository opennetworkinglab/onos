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

/*
 ONOS GUI -- Layer -- Veil Service - Unit Tests
 */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';

import { VeilComponent } from './veil.component';
import { ConsoleLoggerService } from '../../consolelogger.service';
import { FnService } from '../../util/fn.service';
import { LogService } from '../../log.service';
import { GlyphService } from '../../svg/glyph.service';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockGlyphService {}

describe('VeilComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({});
        windowMock = <any>{
            location: <any> {
                hostname: 'foo'
            }
        };
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            declarations: [ VeilComponent ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: 'Window', useValue: windowMock },
            ]
        });
        logServiceSpy = TestBed.get(LogService);
    }));

    it('should create', () => {
        const fixture = TestBed.createComponent(VeilComponent);
        const component = fixture.componentInstance;
        expect(component).toBeTruthy();
    });
});
