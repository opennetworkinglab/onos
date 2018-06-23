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
import { LogService } from '../../../../app/log.service';
import { AppsComponent } from '../../../../app/view/apps/apps.component';
import { DialogService } from '../../../../app/fw/layer/dialog.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { IconComponent } from '../../../../app/fw/svg/icon/icon.component';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { KeyService } from '../../../../app/fw/util/key.service';
import { LionService } from '../../../../app/fw/util/lion.service';
import { LoadingService } from '../../../../app/fw/layer/loading.service';
import { PanelService } from '../../../../app/fw/layer/panel.service';
import { ThemeService } from '../../../../app/fw/util/theme.service';
import { UrlFnService } from '../../../../app/fw/remote/urlfn.service';
import { WebSocketService } from '../../../../app/fw/remote/websocket.service';
import { of } from 'rxjs';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockDialogService {}

class MockFnService {}

class MockIconService {
    loadIconDef() {}
}

class MockKeyService {}

class MockLoadingService {
    startAnim() {}
    stop() {}
    waiting() {}
}

class MockPanelService {}

class MockTableBuilderService {}

class MockThemeService {}

class MockUrlFnService {}

class MockWebSocketService {
    createWebSocket() {}
    isConnected() { return false; }
    unbindHandlers() {}
    bindHandlers() {}
}

/**
 * ONOS GUI -- Apps View -- Unit Tests
 */
describe('AppsComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: AppsComponent;
    let fixture: ComponentFixture<AppsComponent>;
    const bundleObj = {
        'core.view.App': {
            test: 'test1'
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(async(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({'debug': 'txrx'});

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
            declarations: [ AppsComponent, IconComponent ],
            providers: [
                { provide: DialogService, useClass: MockDialogService },
                { provide: FnService, useValue: fs },
                { provide: IconService, useClass: MockIconService },
                { provide: KeyService, useClass: MockKeyService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array()
                        };
                    })
                },
                { provide: LoadingService, useClass: MockLoadingService },
                { provide: LogService, useValue: logSpy },
                { provide: PanelService, useClass: MockPanelService },
                { provide: ThemeService, useClass: MockThemeService },
                { provide: UrlFnService, useClass: MockUrlFnService },
                { provide: WebSocketService, useClass: MockWebSocketService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
        .compileComponents();
        logServiceSpy = TestBed.get(LogService);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(AppsComponent);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
