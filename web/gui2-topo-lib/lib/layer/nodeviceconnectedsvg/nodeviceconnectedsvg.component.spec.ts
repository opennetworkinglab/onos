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
import { ActivatedRoute, Params } from '@angular/router';
import { NoDeviceConnectedSvgComponent } from './nodeviceconnectedsvg.component';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import {
    FnService,
    IconService,
    LionService,
    LogService,
    UrlFnService,
    TableFilterPipe,
    IconComponent,
    WebSocketService, SvgUtilService, PrefsService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

class MockSvgUtilService {
    translate() {}
    scale() {}
}

class MockPrefsService {
}


/**
 * ONOS GUI -- Topology NoDevicesConnected -- Unit Tests
 */
describe('NoDeviceConnectedSvgComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: NoDeviceConnectedSvgComponent;
    let fixture: ComponentFixture<NoDeviceConnectedSvgComponent>;


    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'panel'});

        windowMock = <any>{
            innerWidth: 800,
            innerHeight: 600,
        };
        fs = new FnService(ar, logSpy, windowMock);

        const bundleObj = {
            'core.view.Topo': {
                test: 'test1'
            }
        };
        const mockLion = (key) => {
            return bundleObj[key] || '%' + key + '%';
        };

        TestBed.configureTestingModule({
            declarations: [ NoDeviceConnectedSvgComponent ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: SvgUtilService, useClass: MockSvgUtilService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: PrefsService, useClass: MockPrefsService },
                {
                    provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
                { provide: 'Window', useValue: windowMock },
            ]
        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(NoDeviceConnectedSvgComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
