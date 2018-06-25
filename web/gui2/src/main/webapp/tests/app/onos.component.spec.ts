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
import { TestBed, async } from '@angular/core/testing';
import { RouterModule, RouterOutlet, ChildrenOutletContexts, ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';

import { LogService } from '../../app/log.service';
import { ConsoleLoggerService } from '../../app/consolelogger.service';

import { IconComponent } from '../../app/fw/svg/icon/icon.component';
import { MastComponent } from '../../app/fw/mast/mast/mast.component';
import { NavComponent } from '../../app/fw/nav/nav/nav.component';
import { OnosComponent } from '../../app/onos.component';
import { VeilComponent } from '../../app/fw/layer/veil/veil.component';

import { DialogService } from '../../app/fw/layer/dialog.service';
import { EeService } from '../../app/fw/util/ee.service';
import { FnService } from '../../app/fw/util/fn.service';
import { GlyphService } from '../../app/fw/svg/glyph.service';
import { IconService } from '../../app/fw/svg/icon.service';
import { KeyService } from '../../app/fw/util/key.service';
import { LionService } from '../../app/fw/util/lion.service';
import { NavService } from '../../app/fw/nav/nav.service';
import { OnosService } from '../../app/onos.service';
import { QuickHelpService } from '../../app/fw/layer/quickhelp.service';
import { SvgUtilService } from '../../app/fw/svg/svgutil.service';
import { ThemeService } from '../../app/fw/util/theme.service';
import { SpriteService } from '../../app/fw/svg/sprite.service';
import { WebSocketService, WsOptions } from '../../app/fw/remote/websocket.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockDialogService {}

class MockEeService {}

class MockGlyphService {}

class MockIconService {}

class MockKeyService {}

class MockLionService {}

class MockNavService {}

class MockOnosService {}

class MockQuickHelpService {}

class MockSpriteService {}

class MockThemeService {}

class MockVeilComponent {}

class MockWebSocketService {
    createWebSocket() {}
    isConnected() { return false; }
}

/**
 * ONOS GUI -- Onos Component - Unit Tests
 */
describe('OnosComponent', () => {
    let log: LogService;
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let fixture;
    let app;

    beforeEach(async(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute({'debug': 'TestService'});

        windowMock = <any>{
            location: <any> {
                hostname: '',
                host: '',
                port: '',
                protocol: '',
                search: { debug: 'true'},
                href: ''
            },
            innerHeight: 240,
            innerWidth: 320
        };
        fs = new FnService(ar, log, windowMock);

        TestBed.configureTestingModule({
            declarations: [
                IconComponent,
                MastComponent,
                NavComponent,
                OnosComponent,
                VeilComponent,
                RouterOutlet
            ],
            providers: [
                { provide: ChildrenOutletContexts, useClass: ChildrenOutletContexts },
                { provide: DialogService, useClass: MockDialogService },
                { provide: EeService, useClass: MockEeService },
                { provide: FnService, useValue: fs },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: IconService, useClass: MockIconService },
                { provide: KeyService, useClass: MockKeyService },
                { provide: LionService, useClass: MockLionService },
                { provide: LogService, useValue: log },
                { provide: NavService, useClass: MockNavService },
                { provide: OnosService, useClass: MockOnosService },
                { provide: QuickHelpService, useClass: MockQuickHelpService },
                { provide: SpriteService, useClass: MockSpriteService },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: Window, useFactory: (() => windowMock ) },
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(OnosComponent);
        app = fixture.componentInstance;
    }));

    it('should create the app', async(() => {
        expect(app).toBeTruthy();
    }));

//    it(`should have as title 'onos'`, async(() => {
//        const fixture = TestBed.createComponent(OnosComponent);
//        const app = fixture.componentInstance;
//        expect(app.title).toEqual('onos');
//    }));
});
