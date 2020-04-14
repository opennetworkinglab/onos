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
import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { NavComponent } from './nav/nav.component';
import { OnosComponent } from './onos.component';

import {
    LogService, ConsoleLoggerService,
    ConfirmComponent,
    IconComponent,
    MastComponent,
    VeilComponent,
    FnService,
    GlyphService,
    IconService,
    LionService,
    NavService, UiView,
    OnosService,
    SvgUtilService,
    ThemeService,
    WebSocketService,
    WsOptions
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockDialogService {}

class MockGlyphService {}

class MockIconService {}

class MockLionService {}

class MockNavService {
    uiPlatformViews = new Array<UiView>();
    uiNetworkViews = new Array<UiView>();
    uiOtherViews = new Array<UiView>();
    uiHiddenViews = new Array<UiView>();
}

class MockOnosService {}

class MockThemeService {}

class MockVeilComponent {}

class MockWebSocketService {
    createWebSocket() { }
    isConnected() { return false; }
    unbindHandlers() { }
    bindHandlers() { }
}

/**
 * ONOS GUI -- Onos Component - Unit Tests
 */
describe('OnosComponent', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let component: OnosComponent;
    let fixture: ComponentFixture<OnosComponent>;

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'TestService'});

        windowMock = <any>{
            location: <any>{
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true' },
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            },
            innerHeight: 240,
            innerWidth: 320
        };
        fs = new FnService(ar, logSpy, windowMock);

        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule,
                BrowserModule,
                BrowserAnimationsModule,
                FormsModule
            ],
            declarations: [
                ConfirmComponent,
                IconComponent,
                MastComponent,
                NavComponent,
                OnosComponent,
                VeilComponent,
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: IconService, useClass: MockIconService },
                { provide: LionService, useClass: MockLionService },
                { provide: LogService, useValue: logSpy },
                { provide: NavService, useClass: MockNavService },
                { provide: OnosService, useClass: MockOnosService },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: Window, useFactory: (() => windowMock ) },
            ]
        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(OnosComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

// TODO: Reimplemt this - it's compaining about "no provider for Window"
//    it('should create the component', () => {
//        expect(component).toBeTruthy();
//    });

//    it(`should have as title 'onos'`, async(() => {
//        const fixture = TestBed.createComponent(OnosComponent);
//        const app = fixture.componentInstance;
//        expect(app.title).toEqual('onos');
//    }));
});
