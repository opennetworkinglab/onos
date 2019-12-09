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
import { ZoomableDirective } from './zoomable.directive';
import {inject, TestBed} from '@angular/core/testing';
import {ElementRef} from '@angular/core';
import {ActivatedRoute, Params} from '@angular/router';
import {of} from 'rxjs';
import {FnService} from '../util/fn.service';
import {LogService} from '../log.service';
import {ConsoleLoggerService} from '../consolelogger.service';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

describe('ZoomableDirective', () => {
    let fs: FnService;
    let ar: MockActivatedRoute;
    let log: LogService;
    let mockWindow: Window;

    beforeEach(() => {
        log = new ConsoleLoggerService();
        ar = new MockActivatedRoute({ 'debug': 'txrx' });

        mockWindow = <any>{
            navigator: {
                userAgent: 'HeadlessChrome',
                vendor: 'Google Inc.'
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

        fs = new FnService(ar, log, mockWindow);

        TestBed.configureTestingModule({
            providers: [ZoomableDirective,
                { provide: FnService, useValue: fs },
                { provide: LogService, useValue: log },
                { provide: 'Window', useValue: mockWindow },
                { provide: ElementRef, useValue: mockWindow }
            ]
        });
    });

    it('should create an instance', inject([ZoomableDirective], (directive: ZoomableDirective) => {

        expect(directive).toBeTruthy();
    }));
});
