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

import { PortDetailsComponent } from './portdetails.component';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs/index';
import {
    FnService,
    IconService,
    LogService,
    WebSocketService,
    IconComponent
} from 'gui2-fw-lib';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    classes = 'active-close';
    loadIconDef() { }
}

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

describe('PortdetailsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: PortDetailsComponent;
    let fixture: ComponentFixture<PortDetailsComponent>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'panel' });
        windowMock = <any>{
            location: <any>{
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true' },
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            imports: [BrowserAnimationsModule],
            declarations: [PortDetailsComponent, IconComponent],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: logSpy },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]

        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(PortDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an div.close-btn div.top inside a div.container', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div.container div.top div.close-btn'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.port-icon inside a div.top inside a div.container', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div.container div.top div.port-icon'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('');
    });

    it('should have a div.top-content inside a div.top inside a div.container', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div.container div.top div.top-content'));
        expect(divDe).toBeTruthy();
    });

    it('should have a dev.left inside a div.top-tables inside a div.top-content', () => {
        const portDe: DebugElement = fixture.debugElement;
        const divDe = portDe.query(By.css('div.top-content div.top-tables div.left'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('IDDeviceTypeSpeedEnabled');
    });
});
