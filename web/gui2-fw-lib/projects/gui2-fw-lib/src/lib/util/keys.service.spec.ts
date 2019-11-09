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
import {ActivatedRoute, Params} from '@angular/router';

import { KeysService, KeysToken } from './keys.service';
import { FnService } from './fn.service';
import { LogService } from '../log.service';
import { NavService } from '../nav/nav.service';

import {of} from 'rxjs';
import * as d3 from 'd3';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

class MockNavService {}

/*
 ONOS GUI -- Key Handler Service - Unit Tests
 */
describe('KeysService', () => {
    let ar: ActivatedRoute;
    let fs: FnService;
    let ks: KeysService;
    let mockWindow: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    const qhs: any = {};
    let d3Elem: any;
    let elem: any;
    let last: any;

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

        d3Elem = d3.select('body').append('p').attr('id', 'ptest');
        elem = d3Elem.node();
        last = {
            view: null,
            key: null,
            code: null,
            ev: null
        };

        TestBed.configureTestingModule({
            providers: [KeysService,
                { provide: FnService, useValue: fs},
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: NavService, useClass: MockNavService},
                { provide: 'Window', useFactory: (() => mockWindow ) }
            ]
        });
        ks = TestBed.get(KeysService);
        ks.installOn(d3Elem);
        logServiceSpy = TestBed.get(LogService);
    });

    afterEach(() => {
        d3.select('#ptest').remove();
    });

    it('should be created', () => {
        expect(ks).toBeTruthy();
    });

    it('should define api functions', () => {
        expect(fs.areFunctions(ks, [
            'installOn', 'keyBindings', 'unbindKeys', 'dialogKeys',
            'addSeq', 'remSeq', 'gestureNotes', 'enableKeys', 'enableGlobalKeys',
            'checkNotGlobal', 'getKeyBindings',
            'matchSeq', 'whatKey', 'textFieldInput', 'keyIn', 'qhlion', 'qhlionShowHide',
            'qhlionHintEsc', 'qhlionHintT', 'setupGlobalKeys', 'quickHelp',
            'escapeKey', 'toggleTheme', 'filterMaskedKeys', 'unexParam',
            'setKeyBindings', 'bindDialogKeys', 'unbindDialogKeys'
        ])).toBeTruthy();
    });

    function jsKeyDown(element, code: string, keyName: string) {
        const ev = new KeyboardEvent('keydown',
            { code: code, key: keyName });

        // Chromium Hack
        // if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1) {
        //     Object.defineProperty(ev, 'keyCode', {
        //         get: () => { return this.keyCodeVal; }
        //     });
        //     Object.defineProperty(ev, 'which', {
        //         get: () => { return this.keyCodeVal; }
        //     });
        // }

        if (ev.code !== code.toString()) {
            console.warn('keyCode mismatch ' + ev.code +
                '(' + ev.toString() + ') -> ' + code);
        }
        element.dispatchEvent(ev);
    }

    // === Key binding related tests
    it('should start with default key bindings', () => {
        const state = ks.getKeyBindings();
        const gk = state.globalKeys;
        const mk = state.maskedKeys;
        const vk = state.viewKeys;
        const vf = state.viewFunction;

        expect(gk.length).toEqual(4);
        ['backSlash', 'slash', 'esc', 'T'].forEach((k) => {
            expect(fs.contains(gk, k)).toBeTruthy();
        });

        expect(mk.length).toEqual(3);
        ['backSlash', 'slash', 'T'].forEach((k) => {
            expect(fs.contains(mk, k)).toBeTruthy();
        });

        expect(vk.length).toEqual(0);
        expect(vf).toBeFalsy();
    });

    function bindTestKeys(withDescs?) {
        const keys = ['A', '1', 'F5', 'equals'];
        const kb = {};

        function cb(view, key, code, ev) {
            last.view = view;
            last.key = key;
            last.code = code;
            last.ev = ev;
        }

        function bind(k) {
            return withDescs ?
                [(view, key, code, ev) => {cb(view, key, code, ev); }, 'desc for key ' + k] :
                (view, key, code, ev) => {cb(view, key, code, ev); };
        }

        keys.forEach((k) => {
            kb[k] = bind(k);
        });

        ks.keyBindings(kb);
    }

    function verifyCall(key, code) {
        // TODO: update expectation, when view tokens are implemented
        expect(last.view).toEqual(KeysToken.KEYEV);
        last.view = null;

        expect(last.key).toEqual(key);
        last.key = null;

        expect(last.code).toEqual(code);
        last.code = null;

        expect(last.ev).toBeTruthy();
        last.ev = null;
    }

    function verifyNoCall() {
        expect(last.view).toBeNull();
        expect(last.key).toBeNull();
        expect(last.code).toBeNull();
        expect(last.ev).toBeNull();
    }

    function verifyTestKeys() {
        jsKeyDown(elem, '65', 'A'); // 'A'
        verifyCall('A', '65');
        jsKeyDown(elem, '66', 'B'); // 'B'
        verifyNoCall();

        jsKeyDown(elem, '49', '1'); // '1'
        verifyCall('1', '49');
        jsKeyDown(elem, '50', '2'); // '2'
        verifyNoCall();

        jsKeyDown(elem, '116', 'F5'); // 'F5'
        verifyCall('F5', '116');
        jsKeyDown(elem, '117', 'F6'); // 'F6'
        verifyNoCall();

        jsKeyDown(elem, '187', '='); // 'equals'
        verifyCall('equals', '187');
        jsKeyDown(elem, '189', '-'); // 'dash'
        verifyNoCall();

        const vk = ks.getKeyBindings().viewKeys;

        expect(vk.length).toEqual(4);
        ['A', '1', 'F5', 'equals'].forEach((k) => {
            expect(fs.contains(vk, k)).toBeTruthy();
        });

        expect(ks.getKeyBindings().viewFunction).toBeFalsy();
    }

    it('should allow specific key bindings', () => {
        bindTestKeys();
        verifyTestKeys();
    });

    it('should allow specific key bindings with descriptions', () => {
        bindTestKeys(true);
        verifyTestKeys();
    });

    it('should warn about masked keys', () => {
        const k = {
            'space': (token, key, code, ev) => cb(token, key, code, ev),
            'T': (token, key, code, ev) => cb(token, key, code, ev)
        };
        let count = 0;

        function cb(token, key, code, ev) {
            count++;
            // console.debug('count = ' + count, token, key, code);
        }

        ks.keyBindings(k);

        expect(logServiceSpy.warn).toHaveBeenCalledWith('setKeyBindings()\n: Key "T" is reserved');

        // the 'T' key should NOT invoke our callback
        expect(count).toEqual(0);
        jsKeyDown(elem, '84', 'T'); // 'T'
        expect(count).toEqual(0);

        // but the 'space' key SHOULD invoke our callback
        jsKeyDown(elem, '32', ' '); // 'space'
        expect(count).toEqual(1);
    });

    it('should block keys when disabled', () => {
        let cbCount = 0;

        function cb() { cbCount++; }

        function pressA() { jsKeyDown(elem, '65', 'A'); }  // 65 == 'A' keycode

        ks.keyBindings({ A: () => cb() });

        expect(cbCount).toBe(0);

        pressA();
        expect(cbCount).toBe(1);

        ks.enableKeys(false);
        pressA();
        expect(cbCount).toBe(1);

        ks.enableKeys(true);
        pressA();
        expect(cbCount).toBe(2);
    });

    // === Gesture notes related tests
    it('should start with no notes', () => {
        expect(ks.gestureNotes()).toEqual([]);
    });

    it('should allow us to add nodes', () => {
        const notes = [
            ['one', 'something about one'],
            ['two', 'description of two']
        ];
        ks.gestureNotes(notes);

        expect(ks.gestureNotes()).toEqual(notes);
    });

    it('should ignore non-arrays', () => {
        ks.gestureNotes({foo: 4});
        expect(ks.gestureNotes()).toEqual([]);
    });

    // Consider adding test to ensure array contains 2-tuples of strings
});
