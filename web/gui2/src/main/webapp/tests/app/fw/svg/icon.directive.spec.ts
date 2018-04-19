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
import { IconDirective } from '../../../../app/fw/svg/icon.directive';
import { LogService } from '../../../../app/log.service';
import { IconService } from '../../../../app/fw/svg/icon.service';
import { GlyphService } from '../../../../app/fw/svg/glyph.service';
import { SvgUtilService } from '../../../../app/fw/svg/svgutil.service';
import { FnService } from '../../../../app/fw//util/fn.service';
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
 * ONOS GUI -- SVG -- Icon Directive - Unit Tests
 */
describe('IconDirective', () => {
    let ar: ActivatedRoute;
    let log: LogService;
    let fs: MockFunctionService;
    let gs: GlyphService;
    let is: IconService;
    let sus: SvgUtilService;
    let directive: IconDirective;

    beforeEach(() => {
        ar = new ActivatedRoute();
        log = new LogService();
        fs = new MockFunctionService(ar, log);
        sus = new MockSvgUtilService(fs, log);
        gs = new GlyphService(log);
        is = new IconService(gs, log, sus);
        directive = new IconDirective(is, log);
    });

    afterEach(() => {
        is = null;
        log = null;
        directive = null;
    });

    it('should create an instance', () => {
        expect(directive).toBeTruthy();
    });
});
