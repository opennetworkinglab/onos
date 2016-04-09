/*
 * Copyright 2015-present Open Networking Laboratory
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

/*
 ONOS GUI -- SVG -- Icon Service - Unit Tests
 */
describe('factory: fw/svg/icon.js', function() {
    var is, d3Elem;

    var viewBox = '0 0 50 50',
        glyphSize = '50',
        iconSize = '20';


    beforeEach(module('onosSvg'));

    beforeEach(inject(function (IconService) {
        is = IconService;
        d3Elem = d3.select('body').append('div').attr('id', 'myDiv');
    }));

    afterEach(function () {
        d3.select('#myDiv').remove();
    });

    it('should define IconService', function () {
        expect(is).toBeDefined();
    });

    function checkElemSize(elem, dim) {
        expect(elem.attr('width')).toEqual(dim);
        expect(elem.attr('height')).toEqual(dim);
    }

    function verifyIconStructure(iconClass, useHref, iSize, vBox, gSize) {
        var isz = iSize || iconSize,
            vbx = vBox || viewBox,
            gsz = gSize || glyphSize;

        var svg = d3Elem.selectAll('svg');
        expect(svg.size()).toBe(1);
        checkElemSize(svg, isz);
        expect(svg.attr('viewBox')).toEqual(vbx);

        var g = svg.selectAll('g');
        expect(g.size()).toBe(1);
        expect(g.classed('icon')).toBeTruthy();
        expect(g.classed(iconClass)).toBeTruthy();

        var rect = g.select('rect');
        expect(rect.size()).toBe(1);
        checkElemSize(rect, gsz);
        expect(rect.attr('rx')).toEqual('5');

        if (useHref) {
            var use = g.select('use');
            expect(use.classed('glyph')).toBeTruthy();
            expect(use.attr('xlink:href')).toEqual(useHref);
            checkElemSize(use, gsz);
        }
    }

    it('should load an icon into a div', function () {
        expect(d3Elem.html()).toEqual('');
        is.loadIconByClass(d3Elem, 'active');
        verifyIconStructure('active', '#checkMark');
    });

    it('should allow us to specify the icon size', function () {
        expect(d3Elem.html()).toEqual('');
        is.loadIconByClass(d3Elem, 'inactive', 32);
        verifyIconStructure('inactive', '#xMark', '32');
    });

    it('should verify triangleUp icon', function () {
        expect(d3Elem.html()).toEqual('');
        is.loadIconByClass(d3Elem, 'upArrow', 10);
        verifyIconStructure('upArrow', '#triangleUp', '10');
    });

    it('should verify triangleDown icon', function () {
        expect(d3Elem.html()).toEqual('');
        is.loadIconByClass(d3Elem, 'downArrow', 10);
        verifyIconStructure('downArrow', '#triangleDown', '10');
    });

    it('should verify no icon is displayed', function () {
        expect(d3Elem.html()).toEqual('');
        is.loadIconByClass(d3Elem, 'tableColSortNone', 10);
        verifyIconStructure('tableColSortNone', null, '10');
    });

});
