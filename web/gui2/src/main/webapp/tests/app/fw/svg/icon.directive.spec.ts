/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { TestBed, inject } from '@angular/core/testing';

import { ElementRef } from '@angular/core';
import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { IconDirective } from '../../../../app/fw/svg/icon.directive';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { SvgUtilService } from '../../../../app/fw/svg/svgutil.service';
import { FnService } from '../../../../app/fw//util/fn.service';
import { ActivatedRoute, Router} from '@angular/router';

class MockFnService {}

class MockGlyphService {}

class MockIconService {}

/**
 * ONOS GUI -- SVG -- Icon Directive - Unit Tests
 */
describe('IconDirective', () => {
    let log: LogService;
    const elementMock = <any>{ };

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [ IconDirective,
                { provide: FnService, useClass: MockFnService },
                { provide: LogService, useValue: log },
                { provide: ElementRef, useValue: elementMock },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: IconService, useClass: MockIconService },
            ]
        });
    });

    afterEach(() => {
        log = null;
    });

    it('should create an instance', inject([IconDirective], (directive: IconDirective) => {
        expect(directive).toBeTruthy();
    }));
});
