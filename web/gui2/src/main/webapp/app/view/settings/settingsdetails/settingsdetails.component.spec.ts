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

import { SettingsDetailsComponent } from './settingsdetails.component';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
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
    loadIconDef() { }
}

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

/**
* ONOS GUI -- Settings Detail Panel View -- Unit Tests
*/
describe('SettingsdetailsComponent', () => {
    let component: SettingsDetailsComponent;
    let fixture: ComponentFixture<SettingsDetailsComponent>;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'panel' });
        windowMock = <any>{
            location: <any>{
                settingsname: 'foo',
                settings: 'foo',
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
            declarations: [SettingsDetailsComponent, IconComponent],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: logSpy },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
            .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(SettingsDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have an onos-icon.close-btn inside a div.top inside a div.container', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div.container div.top onos-icon.close-btn'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.top-content inside a div.container', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div.container div.top-content'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.settings-title-1 inside a div.top-content inside a div.container', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div.container div.top-content div.settings-title-1'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.settings-title-2 inside a div.top-content inside a div.container', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div.container div.top-content div.settings-title-2'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.settings-props inside a div.top-content inside a div.container', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div.container div.top-content div.settings-props'));
        expect(divDe).toBeTruthy();
    });
});
