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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogService } from '../../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../../app/consolelogger.service';
import { NavComponent } from '../../../../../app/fw/nav/nav/nav.component';
import { IconComponent } from '../../../../../app/fw/svg/icon/icon.component';
import { IconService } from '../../../../../app/fw/svg/icon.service';
import { NavService } from '../../../../../app/fw/nav/nav.service';

class MockNavService {}

class MockIconService {}

/**
 * ONOS GUI -- Util -- Navigation Component - Unit Tests
 */
describe('NavComponent', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        TestBed.configureTestingModule({
            declarations: [ NavComponent, IconComponent ],
            providers: [
                { provide: LogService, useValue: log },
                { provide: IconService, useClass: MockIconService },
                { provide: NavService, useClass: MockNavService },
            ]
        });
    });

    it('should create', () => {
        const fixture = TestBed.createComponent(NavComponent);
        const component = fixture.componentInstance;
        expect(component).toBeTruthy();
    });
});
