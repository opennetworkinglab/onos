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

import { LogService } from '../log.service';
import { ConsoleLoggerService } from '../consolelogger.service';
import { FnService } from './fn.service';
import { ActivatedRoute, Params } from '@angular/router';
import { of } from 'rxjs';
import * as d3 from 'd3';

class MockActivatedRoute extends ActivatedRoute {
    constructor(params: Params) {
        super();
        this.queryParams = of(params);
    }
}

/**
 * ONOS GUI -- Util -- General Purpose Functions - Unit Tests
 */
describe('FnService', () => {
    let ar: ActivatedRoute;
    let fs: FnService;
    let mockWindow: Window;
    let logServiceSpy: jasmine.SpyObj<LogService>;

    const someFunction = () => {};
    const someArray = [1, 2, 3];
    const someObject = { foo: 'bar'};
    const someNumber = 42;
    const someString = 'xyyzy';
    const someDate = new Date();
    const stringArray = ['foo', 'bar'];

    beforeEach(() => {
        const logSpy = jasmine.createSpyObj('LogService', ['debug', 'warn']);
        ar = new MockActivatedRoute({'debug': 'TestService'});
        mockWindow = <any>{
            innerWidth: 400,
            innerHeight: 200,
            navigator: {
                userAgent: 'defaultUA'
            }
        };


        TestBed.configureTestingModule({
            providers: [FnService,
                { provide: LogService, useValue: logSpy },
                { provide: ActivatedRoute, useValue: ar },
                { provide: 'Window', useFactory: (() => mockWindow ) }
            ]
        });

        fs = TestBed.get(FnService);
        logServiceSpy = TestBed.get(LogService);
    });

    it('should be created', () => {
        expect(fs).toBeTruthy();
    });

    // === Tests for isF()
    it('isF(): null for undefined', () => {
        expect(fs.isF(undefined)).toBeNull();
    });

    it('isF(): null for null', () => {
        expect(fs.isF(null)).toBeNull();
    });
    it('isF(): the reference for function', () => {
        expect(fs.isF(someFunction)).toBe(someFunction);
    });
    it('isF(): null for string', () => {
        expect(fs.isF(someString)).toBeNull();
    });
    it('isF(): null for number', () => {
        expect(fs.isF(someNumber)).toBeNull();
    });
    it('isF(): null for Date', () => {
        expect(fs.isF(someDate)).toBeNull();
    });
    it('isF(): null for array', () => {
        expect(fs.isF(someArray)).toBeNull();
    });
    it('isF(): null for object', () => {
        expect(fs.isF(someObject)).toBeNull();
    });

    // === Tests for isA()
    it('isA(): null for undefined', () => {
        expect(fs.isA(undefined)).toBeNull();
    });
    it('isA(): null for null', () => {
        expect(fs.isA(null)).toBeNull();
    });
    it('isA(): null for function', () => {
        expect(fs.isA(someFunction)).toBeNull();
    });
    it('isA(): null for string', () => {
        expect(fs.isA(someString)).toBeNull();
    });
    it('isA(): null for number', () => {
        expect(fs.isA(someNumber)).toBeNull();
    });
    it('isA(): null for Date', () => {
        expect(fs.isA(someDate)).toBeNull();
    });
    it('isA(): the reference for array', () => {
        expect(fs.isA(someArray)).toBe(someArray);
    });
    it('isA(): null for object', () => {
        expect(fs.isA(someObject)).toBeNull();
    });

    // === Tests for isS()
    it('isS(): null for undefined', () => {
        expect(fs.isS(undefined)).toBeNull();
    });
    it('isS(): null for null', () => {
        expect(fs.isS(null)).toBeNull();
    });
    it('isS(): null for function', () => {
        expect(fs.isS(someFunction)).toBeNull();
    });
    it('isS(): the reference for string', () => {
        expect(fs.isS(someString)).toBe(someString);
    });
    it('isS(): null for number', () => {
        expect(fs.isS(someNumber)).toBeNull();
    });
    it('isS(): null for Date', () => {
        expect(fs.isS(someDate)).toBeNull();
    });
    it('isS(): null for array', () => {
        expect(fs.isS(someArray)).toBeNull();
    });
    it('isS(): null for object', () => {
        expect(fs.isS(someObject)).toBeNull();
    });

    // === Tests for isO()
    it('isO(): null for undefined', () => {
        expect(fs.isO(undefined)).toBeNull();
    });
    it('isO(): null for null', () => {
        expect(fs.isO(null)).toBeNull();
    });
    it('isO(): null for function', () => {
        expect(fs.isO(someFunction)).toBeNull();
    });
    it('isO(): null for string', () => {
        expect(fs.isO(someString)).toBeNull();
    });
    it('isO(): null for number', () => {
        expect(fs.isO(someNumber)).toBeNull();
    });
    it('isO(): null for Date', () => {
        expect(fs.isO(someDate)).toBeNull();
    });
    it('isO(): null for array', () => {
        expect(fs.isO(someArray)).toBeNull();
    });
    it('isO(): the reference for object', () => {
        expect(fs.isO(someObject)).toBe(someObject);
    });


    // === Tests for contains()
    it('contains(): false for non-array', () => {
        expect(fs.contains(null, 1)).toBeFalsy();
    });
    it('contains(): true for contained item', () => {
        expect(fs.contains(someArray, 1)).toBeTruthy();
        expect(fs.contains(stringArray, 'bar')).toBeTruthy();
    });
    it('contains(): false for non-contained item', () => {
        expect(fs.contains(someArray, 109)).toBeFalsy();
        expect(fs.contains(stringArray, 'zonko')).toBeFalsy();
    });

    // === Tests for areFunctions()
    it('areFunctions(): true for empty-array', () => {
        expect(fs.areFunctions({}, [])).toBeTruthy();
    });
    it('areFunctions(): true for some api', () => {
        expect(fs.areFunctions({
            a: () => {},
            b: () => {}
        }, ['b', 'a'])).toBeTruthy();
    });
    it('areFunctions(): false for some other api', () => {
        expect(fs.areFunctions({
            a: () => {},
            b: 'not-a-function'
        }, ['b', 'a'])).toBeFalsy();
    });
    it('areFunctions(): extraneous stuff NOT ignored', () => {
        expect(fs.areFunctions({
            a: () => {},
            b: () => {},
            c: 1,
            d: 'foo'
        }, ['a', 'b'])).toBeFalsy();
    });
    it('areFunctions(): extraneous stuff ignored (alternate fn)', () => {
        expect(fs.areFunctionsNonStrict({
            a: () => {},
            b: () => {},
            c: 1,
            d: 'foo'
        }, ['a', 'b'])).toBeTruthy();
    });

    // == use the now-tested areFunctions() on our own api:
    it('should define api functions', () => {
        expect(fs.areFunctions(fs, [
            'isF', 'isA', 'isS', 'isO', 'contains',
            'areFunctions', 'areFunctionsNonStrict', 'windowSize',
            'isMobile', 'isChrome', 'isChromeHeadless', 'isSafari',
            'isFirefox', 'parseDebugFlags',
            'debugOn', 'debug', 'find', 'inArray', 'removeFromArray',
            'isEmptyObject', 'cap', 'noPx', 'noPxStyle', 'endsWith',
            'inEvilList', 'analyze', 'sanitize', 'sameObjProps', 'containsObj',
            'addToTrie', 'removeFromTrie', 'trieLookup'
//            'find', 'inArray', 'removeFromArray', 'isEmptyObject', 'sameObjProps', 'containsObj', 'cap',
//            'eecode', 'noPx', 'noPxStyle', 'endsWith', 'addToTrie', 'removeFromTrie', 'trieLookup',
//            'classNames', 'extend', 'sanitize'
        ])).toBeTruthy();
    });


    // === Tests for windowSize()
    it('windowSize(): adjust height', () => {
        const dim = fs.windowSize(50);
        expect(dim.width).toEqual(400);
        expect(dim.height).toEqual(150);
    });

    it('windowSize(): adjust width', () => {
        const dim = fs.windowSize(0, 50);
        expect(dim.width).toEqual(350);
        expect(dim.height).toEqual(200);
    });

    it('windowSize(): adjust width and height', () => {
        const dim = fs.windowSize(101, 201);
        expect(dim.width).toEqual(199);
        expect(dim.height).toEqual(99);
    });

    // === Tests for isMobile()
    const uaMap = {
        chrome: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) ' +
                'AppleWebKit/537.36 (KHTML, like Gecko) ' +
                'Chrome/41.0.2272.89 Safari/537.36',

        iPad: 'Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) ' +
                'AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 ' +
                'Mobile/11A465 Safari/9537.53',

        iPhone: 'Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) ' +
                'AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 ' +
                'Mobile/11A465 Safari/9537.53'
    };

    function setUa(key) {
        const str = uaMap[key];
        expect(str).toBeTruthy();
        (<any>mockWindow.navigator).userAgent = str;
    }

    it('isMobile(): should be false for Chrome on Mac OS X', () => {
        setUa('chrome');
        expect(fs.isMobile()).toBe(false);
    });
    it('isMobile(): should be true for Safari on iPad', () => {
        setUa('iPad');
        expect(fs.isMobile()).toBe(true);
    });
    it('isMobile(): should be true for Safari on iPhone', () => {
        setUa('iPhone');
        expect(fs.isMobile()).toBe(true);
    });

    // === Tests for find()
    const dataset = [
        { id: 'foo', name: 'Furby'},
        { id: 'bar', name: 'Barbi'},
        { id: 'baz', name: 'Basil'},
        { id: 'goo', name: 'Gabby'},
        { id: 'zoo', name: 'Zevvv'}
    ];

    it('should not find ooo', () => {
        expect(fs.find('ooo', dataset)).toEqual(-1);
    });
    it('should find foo', () => {
        expect(fs.find('foo', dataset)).toEqual(0);
    });
    it('should find zoo', () => {
        expect(fs.find('zoo', dataset)).toEqual(4);
    });

    it('should not find Simon', () => {
        expect(fs.find('Simon', dataset, 'name')).toEqual(-1);
    });
    it('should find Furby', () => {
        expect(fs.find('Furby', dataset, 'name')).toEqual(0);
    });
    it('should find Zevvv', () => {
        expect(fs.find('Zevvv', dataset, 'name')).toEqual(4);
    });


    // === Tests for inArray()
    const objRef = { x: 1, y: 2 };
    const array = [1, 3.14, 'hey', objRef, 'there', true];
    const array2 = ['b', 'a', 'd', 'a', 's', 's'];

    it('should not find HOO', () => {
        expect(fs.inArray('HOO', array)).toEqual(-1);
    });
    it('should find 1', () => {
        expect(fs.inArray(1, array)).toEqual(0);
    });
    it('should find pi', () => {
        expect(fs.inArray(3.14, array)).toEqual(1);
    });
    it('should find hey', () => {
        expect(fs.inArray('hey', array)).toEqual(2);
    });
    it('should find the object', () => {
        expect(fs.inArray(objRef, array)).toEqual(3);
    });
    it('should find there', () => {
        expect(fs.inArray('there', array)).toEqual(4);
    });
    it('should find true', () => {
        expect(fs.inArray(true, array)).toEqual(5);
    });

    it('should find the first occurrence A', () => {
        expect(fs.inArray('a', array2)).toEqual(1);
    });
    it('should find the first occurrence S', () => {
        expect(fs.inArray('s', array2)).toEqual(4);
    });
    it('should not find X', () => {
        expect(fs.inArray('x', array2)).toEqual(-1);
    });

    // === Tests for removeFromArray()
    it('should keep the array the same, for non-match', () => {
        const array1 = [1, 2, 3];
        expect(fs.removeFromArray(4, array1)).toBe(false);
        expect(array1).toEqual([1, 2, 3]);
    });
    it('should remove a value', () => {
        const array1a = [1, 2, 3];
        expect(fs.removeFromArray(2, array1a)).toBe(true);
        expect(array1a).toEqual([1, 3]);
    });
    it('should remove the first occurrence', () => {
        const array1b = ['x', 'y', 'z', 'z', 'y'];
        expect(fs.removeFromArray('y', array1b)).toBe(true);
        expect(array1b).toEqual(['x', 'z', 'z', 'y']);
        expect(fs.removeFromArray('x', array1b)).toBe(true);
        expect(array1b).toEqual(['z', 'z', 'y']);
    });

    // === Tests for isEmptyObject()
    it('should return true if an object is empty', () => {
        expect(fs.isEmptyObject({})).toBe(true);
    });
    it('should return false if an object is not empty', () => {
        expect(fs.isEmptyObject({foo: 'bar'})).toBe(false);
    });

    // === Tests for cap()
    it('should ignore non-alpha', () => {
        expect(fs.cap('123')).toEqual('123');
    });
    it('should capitalize first char', () => {
        expect(fs.cap('Foo')).toEqual('Foo');
        expect(fs.cap('foo')).toEqual('Foo');
        expect(fs.cap('foo bar')).toEqual('Foo bar');
        expect(fs.cap('FOO BAR')).toEqual('Foo bar');
        expect(fs.cap('foo Bar')).toEqual('Foo bar');
    });

    // === Tests for noPx()
    it('should return the value without px suffix', () => {
        expect(fs.noPx('10px')).toBe(10);
        expect(fs.noPx('500px')).toBe(500);
        expect(fs.noPx('-80px')).toBe(-80);
    });

    // === Tests for noPxStyle()
    it('should give a style\'s property without px suffix', () => {
        const d3Elem = d3.select('body')
            .append('div')
            .attr('id', 'fooElem')
            .style('width', '500px')
            .style('height', '200px')
            .style('font-size', '12px');
        expect(fs.noPxStyle(d3Elem, 'width')).toBe(500);
        expect(fs.noPxStyle(d3Elem, 'height')).toBe(200);
        expect(fs.noPxStyle(d3Elem, 'font-size')).toBe(12);
        d3.select('#fooElem').remove();
    });

    // === Tests for endsWith()
    it('should return true if string ends with foo', () => {
        expect(fs.endsWith('barfoo', 'foo')).toBe(true);
    });

    it('should return false if string doesnt end with foo', () => {
        expect(fs.endsWith('barfood', 'foo')).toBe(false);
    });

    // === Tests for sanitize()
    it('should return foo', () => {
        expect(fs.sanitize('foo')).toEqual('foo');
    });
    it('should retain < b > tags', () => {
        const str = 'foo <b>bar</b> baz';
        expect(fs.sanitize(str)).toEqual(str);
    });
    it('should retain < i > tags', () => {
        const str = 'foo <i>bar</i> baz';
        expect(fs.sanitize(str)).toEqual(str);
    });
    it('should retain < p > tags', () => {
        const str = 'foo <p>bar</p> baz';
        expect(fs.sanitize(str)).toEqual(str);
    });
    it('should retain < em > tags', () => {
        const str = 'foo <em>bar</em> baz';
        expect(fs.sanitize(str)).toEqual(str);
    });
    it('should retain < strong > tags', () => {
        const str = 'foo <strong>bar</strong> baz';
        expect(fs.sanitize(str)).toEqual(str);
    });

    it('should reject < a > tags', () => {
        expect(fs.sanitize('test <a href="hah">something</a> this'))
            .toEqual('test something this');
    });

    it('should log a warning for < script > tags', () => {
        expect(fs.sanitize('<script>alert("foo");</script>'))
            .toEqual('');
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'Unsanitary HTML input -- <script> detected!'
        );
    });
    it('should log a warning for < style > tags', () => {
        expect(fs.sanitize('<style> h1 {color:red;} </style>'))
            .toEqual('');
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'Unsanitary HTML input -- <style> detected!'
        );
    });

    it('should log a warning for < iframe > tags', () => {
        expect(fs.sanitize('Foo<iframe><body><h1>fake</h1></body></iframe>Bar'))
            .toEqual('FooBar');
        expect(logServiceSpy.warn).toHaveBeenCalledWith(
            'Unsanitary HTML input -- <iframe> detected!'
        );
    });

    it('should completely strip < script >, remove < a >, retain < i >', () => {
        expect(fs.sanitize('Hey <i>this</i> is <script>alert("foo");</script> <a href="meh">cool</a>'))
            .toEqual('Hey <i>this</i> is  cool');
    });
});
