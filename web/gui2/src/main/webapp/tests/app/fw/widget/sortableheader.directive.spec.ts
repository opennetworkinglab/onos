/*
 * Copyright 2015-present Open Networking Foundation
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

import { LogService } from '../../../../app/log.service';
import { ConsoleLoggerService } from '../../../../app/consolelogger.service';
import { SortableHeaderDirective } from '../../../../app/fw/widget/sortableheader.directive';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { FnService } from '../../../../app/fw/util/fn.service';

class MockFnService {}

class MockGlyphService {}

class MockIconService {}

/**
 * ONOS GUI -- Widget -- Table Sortable Header Directive - Unit Tests
 */
describe('SortableHeaderDirective', () => {
    let log: LogService;

    beforeEach(() => {
        log = new ConsoleLoggerService();

        TestBed.configureTestingModule({
            providers: [ SortableHeaderDirective,
                { provide: FnService, useClass: MockFnService },
                { provide: LogService, useValue: log },
                { provide: GlyphService, useClass: MockGlyphService },
                { provide: IconService, useClass: MockIconService },
            ]
        });
    });

    afterEach(() => {
        log = null;
    });

    it('should create an instance', inject([SortableHeaderDirective], (directive: SortableHeaderDirective) => {
        expect(directive).toBeTruthy();
    }));
});
