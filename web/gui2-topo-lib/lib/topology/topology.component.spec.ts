/*
 * Copyright 2019-present Open Networking Foundation
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
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import * as d3 from 'd3';
import { TopologyComponent } from './topology.component';
import {
    Instance,
    InstanceComponent
} from '../panel/instance/instance.component';
import { SummaryComponent } from '../panel/summary/summary.component';
import { ToolbarComponent } from '../panel/toolbar/toolbar.component';
import { DetailsComponent } from '../panel/details/details.component';
import {Intent, TopologyService} from '../topology.service';

import {
    FlashComponent,
    QuickhelpComponent,
    FnService,
    LogService,
    IconService, IconComponent, PrefsService, KeysService, LionService, ZoomableDirective
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';
import {RouterTestingModule} from '@angular/router/testing';
import {TrafficService} from '../traffic.service';
import {ForceSvgComponent} from '../layer/forcesvg/forcesvg.component';
import {DraggableDirective} from '../layer/forcesvg/draggable/draggable.directive';
import {MapSelectorComponent} from '../panel/mapselector/mapselector.component';
import {BackgroundSvgComponent} from '../layer/backgroundsvg/backgroundsvg.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MapSvgComponent} from '../layer/mapsvg/mapsvg.component';
import {GridsvgComponent} from '../layer/gridsvg/gridsvg.component';
import {LinkSvgComponent} from '../layer/forcesvg/visuals/linksvg/linksvg.component';
import {DeviceNodeSvgComponent} from '../layer/forcesvg/visuals/devicenodesvg/devicenodesvg.component';
import {SubRegionNodeSvgComponent} from '../layer/forcesvg/visuals/subregionnodesvg/subregionnodesvg.component';
import {HostNodeSvgComponent} from '../layer/forcesvg/visuals/hostnodesvg/hostnodesvg.component';
import {LayoutService} from '../layout.service';
import {BadgeSvgComponent} from '../layer/forcesvg/visuals/badgesvg/badgesvg.component';


class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockHttpClient {}

class MockTopologyService {
    init(instance: InstanceComponent) {
        instance.onosInstances = [
            <Instance>{
                'id': 'inst1',
                'ip': '127.0.0.1',
                'reachable': true,
                'online': true,
                'ready': true,
                'switches': 4,
                'uiAttached': true
            },
            <Instance>{
                'id': 'inst1',
                'ip': '127.0.0.2',
                'reachable': true,
                'online': true,
                'ready': true,
                'switches': 3,
                'uiAttached': false
            }
        ];
    }
    destroy() {}
    setSelectedIntent(selectedIntent: Intent): void {}
    selectRelatedIntent(ids: string[]): void {}
    cancelHighlights(): void {}
}

class MockIconService {
    loadIconDef() { }
}

class MockKeysService {
    quickHelpShown: boolean = true;

    keyHandler: {
        viewKeys: any[],
        globalKeys: any[]
    };

    mockViewKeys: Object[];
    constructor() {
        this.mockViewKeys = [];
        this.keyHandler = {
            viewKeys: this.mockViewKeys,
            globalKeys: this.mockViewKeys
        };
    }

    keyBindings(x) {
        return {};
    }

    gestureNotes() {
        return {};
    }
}

class MockTrafficService {
    init(force: ForceSvgComponent) {}
    destroy() {}
}

class MockLayoutService {}

class MockPrefsService {
    listeners: ((data) => void)[] = [];

    getPrefs() {
        return { 'topo2_prefs': ''};
    }

    addListener(listener: (data) => void): void {
        this.listeners.push(listener);
    }

    removeListener(listener: (data) => void) {
        this.listeners = this.listeners.filter((obj) => obj !== listener);
    }

    setPrefs(name: string, obj: Object) {

    }

}

/**
 * ONOS GUI -- Topology View -- Unit Tests
 */
// Skipping temporarily
describe('TopologyComponent', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let windowMock: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let component: TopologyComponent;
    let fixture: ComponentFixture<TopologyComponent>;

    const bundleObj = {
        'core.fw.QuickHelp': {
            test: 'test1',
            tt_help: 'Help!'
        }
    };
    const mockLion = (key) =>  {
        return bundleObj[key] || '%' + key + '%';
    };

    beforeEach(() => {
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
                RouterTestingModule,
                FormsModule,
                ReactiveFormsModule
            ],
            declarations: [
                TopologyComponent,
                InstanceComponent,
                SummaryComponent,
                ToolbarComponent,
                DetailsComponent,
                FlashComponent,
                IconComponent,
                QuickhelpComponent,
                ForceSvgComponent,
                LinkSvgComponent,
                DeviceNodeSvgComponent,
                HostNodeSvgComponent,
                DraggableDirective,
                ZoomableDirective,
                SubRegionNodeSvgComponent,
                MapSelectorComponent,
                BackgroundSvgComponent,
                MapSvgComponent,
                GridsvgComponent,
                BadgeSvgComponent
            ],
            providers: [
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: logSpy },
                { provide: 'Window', useValue: windowMock },
                { provide: HttpClient, useClass: MockHttpClient },
                { provide: TopologyService, useClass: MockTopologyService },
                { provide: TrafficService, useClass: MockTrafficService },
                { provide: LayoutService, useClass: MockLayoutService },
                { provide: IconService, useClass: MockIconService },
                { provide: PrefsService, useClass: MockPrefsService },
                { provide: KeysService, useClass: MockKeysService },
                { provide: LionService, useFactory: (() => {
                        return {
                            bundle: ((bundleId) => mockLion),
                            ubercache: new Array(),
                            loadCbs: new Map<string, () => void>([])
                        };
                    })
                },
            ]
        }).compileComponents();
        logServiceSpy = TestBed.get(LogService);
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(TopologyComponent);
        component = fixture.componentInstance;

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
