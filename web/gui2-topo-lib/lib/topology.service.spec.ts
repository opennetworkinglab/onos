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
import { TestBed, inject } from '@angular/core/testing';
import { ActivatedRoute, Params } from '@angular/router';
import {of} from 'rxjs';

import { TopologyService } from './topology.service';
import {
    LogService,
    FnService
} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Topology Service - Unit Tests
 */
describe('TopologyService', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let ar: ActivatedRoute;
    let fs: FnService;
    let mockWindow: Window;

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['debug', 'warn', 'info']);
        ar = new MockActivatedRoute({'debug': 'TestService'});
        mockWindow = <any>{
            innerWidth: 400,
            innerHeight: 200,
            navigator: {
                userAgent: 'defaultUA'
            },
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
        fs = new FnService(ar, logSpy, mockWindow);

        TestBed.configureTestingModule({
            providers: [TopologyService,
                { provide: FnService, useValue: fs},
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: 'Window', useFactory: (() => mockWindow ) }
            ]
        });
        logServiceSpy = TestBed.get(LogService);
    });

    it('should be created', inject([TopologyService], (service: TopologyService) => {
        expect(service).toBeTruthy();
    }));
});
