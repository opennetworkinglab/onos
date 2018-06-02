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

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { DeviceComponent } from '../../../../app/view/device/device.component';

import { DetailsPanelService } from '../../../../app/fw/layer/detailspanel.service';
import { FnService, WindowSize } from '../../../../app/fw/util/fn.service';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { KeyService } from '../../../../app/fw/util/key.service';
import { LoadingService } from '../../../../app/fw/layer/loading.service';
import { NavService } from '../../../../app/fw/nav/nav.service';
import { MastService } from '../../../../app/fw/mast/mast.service';
import { PanelService } from '../../../../app/fw/layer/panel.service';
import { SvgUtilService } from '../../../../app/fw/svg/svgutil.service';
import { TableBuilderService } from '../../../../app/fw/widget/tablebuilder.service';
import { TableDetailService } from '../../../../app/fw/widget/tabledetail.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';

class MockDetailsPanelService {}

class MockFnService {
    windowSize(offH: number = 0, offW: number = 0): WindowSize {
        return {
            height: 123,
            width: 456
        };
    }
}

class MockIconService {}

class MockGlyphService {}

class MockKeyService {}

class MockLoadingService {
    startAnim() {
        // Do nothing
    }
}

class MockNavService {}

class MockMastService {}

class MockPanelService {}

class MockTableBuilderService {}

class MockTableDetailService {}

class MockWebSocketService {}

/**
 * ONOS GUI -- Device View Module - Unit Tests
 */
describe('DeviceComponent', () => {
    let log: LogService;
    let component: DeviceComponent;
    let fixture: ComponentFixture<DeviceComponent>;
    const windowMock = <any>{ location: <any> { hostname: 'localhost' } };

    beforeEach(async(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            declarations: [ DeviceComponent ],
            providers: [
                { provide: DetailsPanelService, useClass: MockDetailsPanelService },
                { provide: FnService, useClass: MockFnService },
                { provide: IconService, useClass: MockIconService },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: KeyService, useClass: MockKeyService },
                { provide: LoadingService, useClass: MockLoadingService },
                { provide: MastService, useClass: MockMastService },
                { provide: NavService, useClass: MockNavService },
                { provide: LogService, useValue: log },
                { provide: PanelService, useClass: MockPanelService },
                { provide: TableBuilderService, useClass: MockTableBuilderService },
                { provide: TableDetailService, useClass: MockTableDetailService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: Window, useValue: windowMock },
             ]
        })
        .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DeviceComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
