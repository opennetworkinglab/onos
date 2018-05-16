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
import { MastComponent } from '../../../../../app/fw/mast/mast/mast.component';
import { IconComponent } from '../../../../../app/fw/svg/icon/icon.component';
import { DialogService } from '../../../../../app/fw/layer/dialog.service';
import { LionService } from '../../../../../app/fw/util/lion.service';
import { IconService } from '../../../../../app/fw/svg/icon.service';
import { NavService } from '../../../../../app/fw/nav/nav.service';
import { WebSocketService } from '../../../../../app/fw/remote/websocket.service';

class MockDialogService {}

class MockLionService {}

class MockNavService {}

class MockWebSocketService {}

class MockIconService {}

/**
 * ONOS GUI -- Masthead Controller - Unit Tests
 */
describe('MastComponent', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        TestBed.configureTestingModule({
            declarations: [ MastComponent, IconComponent ],
            providers: [
                { provide: DialogService, useClass: MockDialogService },
                { provide: LionService, useClass: MockLionService },
                { provide: LogService, useValue: log },
                { provide: NavService, useClass: MockNavService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: IconService, useClass: MockIconService },
            ]
        });
    });

    it('should create', () => {
        const fixture = TestBed.createComponent(MastComponent);
        const component = fixture.componentInstance;
        expect(component).toBeTruthy();
    });
});
