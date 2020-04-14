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
import { DraggableDirective } from './draggable.directive';
import {inject, TestBed} from '@angular/core/testing';
import {ElementRef} from '@angular/core';
import {LogService} from 'org_onosproject_onos/web/gui2-fw-lib/public_api';

export class MockElementRef extends ElementRef {
    nativeElement = {};
}

describe('DraggableDirective', () => {
    let logServiceSpy: jasmine.SpyObj<LogService>;
    let mockWindow: Window;

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        mockWindow = <any>{
            navigator: {
                userAgent: 'HeadlessChrome',
                vendor: 'Google Inc.'
            }
        };

        TestBed.configureTestingModule({
            providers: [DraggableDirective,
                { provide: LogService, useValue: logSpy },
                { provide: 'Window', useFactory: (() => mockWindow ) },
                { provide: ElementRef, useValue: mockWindow }
            ]
        });
        logServiceSpy = TestBed.get(LogService);
    });

    it('should create an instance', inject([DraggableDirective], (directive: DraggableDirective) => {

        expect(directive).toBeTruthy();
    }));
});
