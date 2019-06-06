/*
 * Copyright ${year}-present Open Networking Foundation
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

import { ${appNameTitle}Component } from './${artifactId}.component';
import { WelcomeComponent } from '../welcome/welcome.component';
import {ActivatedRoute, Params} from '@angular/router';
import {CommonModule} from '@angular/common';
import { of } from 'rxjs';
import { } from 'jasmine';
import {
    Gui2FwLibModule,
    FnService,
    IconService,
    IconComponent,
    LogService,
    TableFilterPipe, LoadingComponent,
} from 'gui2-fw-lib';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockIconService {
    loadIconDef() { }
}

describe('${appNameTitle}Component', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: ${appNameTitle}Component;
    let fixture: ComponentFixture<${appNameTitle}Component>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });

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
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                FormsModule,
                RouterTestingModule,
                Gui2FwLibModule
            ],
            declarations: [
                ${appNameTitle}Component,
                WelcomeComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: IconService, useClass: MockIconService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(${appNameTitle}Component);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
