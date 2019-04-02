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

import { SettingsComponent } from './settings.component';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import {
    FnService,
    IconService,
    IconComponent,
    LogService,
    WebSocketService, LoadingComponent
} from 'gui2-fw-lib';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SettingsDetailsComponent } from '../settingsdetails/settingsdetails.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

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
 * ONOS GUI -- Settings View Module - Unit Tests
 */
describe('SettingsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: SettingsComponent;
    let fixture: ComponentFixture<SettingsComponent>;

    const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
    ar = new MockActivatedRoute({ 'debug': 'txrx' });

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

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [BrowserAnimationsModule],
            declarations: [
                SettingsComponent,
                IconComponent,
                SettingsDetailsComponent,
                LoadingComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: LogService, useValue: logSpy },
                { provide: WebSocketService, useClass: MockWebSocketService },
            ]
        })
            .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(SettingsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have a div.tabular-header inside a div#ov-settings', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.tabular-header'));
        expect(divDe).toBeTruthy();
    });

    it('should have a h2 inside the div.tabular-header', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.tabular-header h2'));
        const div: HTMLElement = divDe.nativeElement;
        expect(div.textContent).toEqual('Component Settings (0 total)');
    });

    it('should have a refresh button inside the div.tabular-header', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.tabular-header div.ctrl-btns div.refresh'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.summary-list inside a div#ov-settings', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.summary-list'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.table-header inside a div.summary-list inside a div#ov-settings', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.summary-list div.table-header'));
        expect(divDe).toBeTruthy();
    });

    it('should have a div.table-body inside a div.summary-list inside a div#ov-settings', () => {
        const settingsDe: DebugElement = fixture.debugElement;
        const divDe = settingsDe.query(By.css('div#ov-settings div.summary-list div.table-body'));
        expect(divDe).toBeTruthy();
    });
});
