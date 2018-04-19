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
import { SortableHeaderDirective } from '../../../../app/fw/widget/sortableheader.directive';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { SvgUtilService } from '../../../../app/fw/svg/svgutil.service';
import { LogService } from '../../../../app/log.service';
import { FnService } from '../../../../app/fw/util/fn.service';
import { ActivatedRoute, Router} from '@angular/router';

class MockGlyphService extends GlyphService {
    // Override things as necessary
}

class MockSvgUtilService extends SvgUtilService {
    // Override things as necessary
}

class MockFunctionService extends FnService {
    // Override things as necessary
}

/**
 * ONOS GUI -- Widget -- Table Sortable Header Directive - Unit Tests
 */
describe('SortableHeaderDirective', () => {
    let gs: MockGlyphService;
    let sus: MockSvgUtilService;
    let icon: IconService;
    let log: LogService;
    let fs: MockFunctionService;
    let ar: ActivatedRoute;
    let directive: SortableHeaderDirective;

    beforeEach(() => {
        log = new LogService();
        ar = new ActivatedRoute();
        fs = new MockFunctionService(ar, log);
        gs = new MockGlyphService(log);
        sus = new MockSvgUtilService(fs, log);
        icon = new IconService(gs, log, sus);
        directive = new SortableHeaderDirective(icon, log);
    });

    afterEach(() => {
        log = null;
        icon = null;
        directive = null;
    });

    it('should create an instance', () => {
        expect(directive).toBeTruthy();
    });
});
