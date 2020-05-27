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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import {
    FnService,
    IconService,
    LionService,
    LogService,
    UrlFnService,
    WebSocketService,
    TableFilterPipe,
    IconComponent
} from 'gui2-fw-lib';

import { AppsDetailsComponent } from './appsdetails.component';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockFnService {}

class MockIconService {
    loadIconDef() {}
}

class MockUrlFnService {}

class MockWebSocketService {
    createWebSocket() {}
    isConnected() { return false; }
    unbindHandlers() {}
    bindHandlers() {}
}

/**
 * ONOS GUI -- Apps Detail Panel View -- Unit Tests
 */
describe('AppsDetailsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: AppsDetailsComponent;
    let fixture: ComponentFixture<AppsDetailsComponent>;
    const bundleObj = {
        'core.view.App': {
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'panel'});

        windowMock = <any>{
            location: <any> {
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true'},
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            imports: [ BrowserAnimationsModule ],
            declarations: [ AppsDetailsComponent, IconComponent ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs:  new Map<string, () => void>([])
                        };
                    })
                },
                { provide: LogService, useValue: logSpy },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(AppsDetailsComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an onos-icon.close-btn inside a div.top inside a div.container', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.container div.top onos-icon.close-btn'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.top-content inside a div.top inside a div.container', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.container div.top div.top-content'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.app-title inside a div.top-content', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.top-content div.app-title'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('');
    });

    it('should have an img inside a div.left div.top-content', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.top-content div.left.app-icon img'));
        expect(divDe).toBeTruthy();
    });

    it('should have a table.app-props inside a div.right inside a div.top-content', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.top-content div.right table.app-props'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('%app_id%%state%%category%%version%%origin%%role%');
    });

    it('should have an a inside an div.app-url inside a div.top-content', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.top-content div.app-url a'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.app-readme inside a div.middle inside a div.container', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.container div.middle div.app-readme'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.features inside a div.bottom inside a div.container', () => {
        const appDe: DebugElement = fixture.debugElement;
        const divDe = appDe.query(By.css('div.container div.bottom div.features'));
        expect(divDe).toBeTruthy();
    });
});
