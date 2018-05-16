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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { AppsComponent } from '../../../../app/view/apps/apps.component';
import { DialogService } from '../../../../app/fw/layer/dialog.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { KeyService } from '../../../../app/fw/util/key.service';
import { LionService } from '../../../../app/fw/util/lion.service';
import { PanelService } from '../../../../app/fw/layer/panel.service';
import { TableBuilderService } from '../../../../app/fw/widget/tablebuilder.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';

class MockDialogService {}

class MockFnService {}

class MockIconService {}

class MockKeyService {}

class MockLionService {}

class MockPanelService {}

class MockTableBuilderService {}

class MockUrlFnService {}

class MockWebSocketService {}

/**
 * ONOS GUI -- Apps View -- Unit Tests
 */
describe('AppsComponent', () => {
    let log: LogService;
    let component: AppsComponent;
    let fixture: ComponentFixture<AppsComponent>;
    const windowMock = <any>{ location: <any> { hostname: 'localhost' } };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            declarations: [ AppsComponent ],
            providers: [
                { provide: DialogService, useClass: MockDialogService },
                { provide: FnService, useClass: MockFnService },
                { provide: IconService, useClass: MockIconService },
                { provide: KeyService, useClass: MockKeyService },
                { provide: LionService, useClass: MockLionService },
                { provide: LogService, useValue: log },
                { provide: PanelService, useClass: MockPanelService },
                { provide: TableBuilderService, useClass: MockTableBuilderService },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: Window, useValue: windowMock },
            ]
        })
        .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(AppsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
