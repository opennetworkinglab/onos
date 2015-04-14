/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Widget -- Table Service - Unit Tests
 */
describe('factory: fw/widget/table.js', function () {
    var $log, $compile, $rootScope,
        fs, mast, is,
        scope, compiled,
        table, thead, tbody, mockHeader,
        mockh2Height = '10px',
        tableIconTdSize = 100,
        pdgTop = 101,
        numTestElems = 4;

    var onosFixedHeaderTags = '<table ' +
                                'onos-fixed-header>' +
                                '<thead>' +
                                '<tr>' +
                                '<th></th>' +
                                '<th>Device ID </th>' +
                                '<th col-width="100px">H/W Version </th>' +
                                '<th>S/W Version </th>' +
                                '</tr>' +
                                '</thead>' +
                                '<tbody>' +
                                '<tr>' +
                                '<td colspan="4">' +
                                    'No Devices found' +
                                '</td>' +
                                '</tr>' +
                                '<tr>' +
                                '<td class="table-icon">' +
                                    '<div icon icon-id="{{dev._iconid_available}}">' +
                                    '</div>' +
                                '</td>' +
                                '<td>Some ID</td>' +
                                '<td>Some HW</td>' +
                                '<td>Some Software</td>' +
                                '</tr>' +
                                '</tbody>' +
                                '</table>',

        onosSortableHeaderTags = '<table ' +
                                'onos-sortable-header ' +
                                'sort-callback="sortCallback(requestParams)">' +
                                '<thead>' +
                                '<tr>' +
                                '<th colId="available"></th>' +
                                '<th colId="id" sortable>Device ID </th>' +
                                '<th colId="hw" sortable>H/W Version </th>' +
                                '<th colId="sw" sortable>S/W Version </th>' +
                                '</tr>' +
                                '</thead>' +
                                '<tbody>' +
                                '<tr>' +
                                '<td>' +
                                    '<div icon icon-id="{{dev._iconid_available}}">' +
                                    '</div>' +
                                '</td>' +
                                '<td>Some ID</td>' +
                                '<td>Some HW</td>' +
                                '<td>Some Software</td>' +
                                '</tr>' +
                                '</tbody>' +
                                '</table>';

    beforeEach(module('onosWidget', 'onosUtil', 'onosMast', 'onosSvg'));

    var mockWindow = {
        innerWidth: 400,
        innerHeight: 200,
        navigator: {
            userAgent: 'defaultUA'
        },
        on: function () {},
        addEventListener: function () {}
    };

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('$window', mockWindow);
        });
    });

    beforeEach(inject(function (_$log_, _$compile_, _$rootScope_,
                                FnService, MastService, IconService) {
        $log = _$log_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        fs = FnService;
        mast = MastService;
        is = IconService;
    }));

    beforeEach(function () {
        scope = $rootScope.$new();
    });

    beforeEach(function () {
        mockHeader = d3.select('body')
            .append('h2')
            .style('height', mockh2Height)
            .html('Some Header');
    });

    afterEach(function () {
        table = null;
        thead = null;
        tbody = null;
        mockHeader.remove();
    });

    function compileTable() {
        compiled = $compile(table);
        compiled(scope);
        scope.$digest();
    }

    function verifyGivenTags(dirName) {
        expect(table).toBeDefined();
        expect(table.attr(dirName)).toBe('');

        thead = table.find('thead');
        expect(thead).toBeDefined();
        tbody = table.find('tbody');
        expect(tbody).toBeDefined();
    }

    function verifyCssDisplay() {
        var tableHeight = fs.windowSize(pdgTop).height;

        expect(thead.css('display')).toBe('block');
        expect(tbody.css('display')).toBe('block');
        expect(tbody.css('height')).toBe(tableHeight + 'px');
        expect(tbody.css('overflow')).toBe('auto');
    }

    function verifyColWidth() {
        var winWidth = fs.windowSize().width,
            colWidth, thElems, tr, tdElem;

        colWidth = Math.floor(winWidth / numTestElems);

        thElems = thead.find('th');

        angular.forEach(thElems, function (thElem, i) {
            thElem = angular.element(thElems[i]);
            tr = angular.element(tbody.find('tr').eq(1));
            tdElem = angular.element(tr.find('td').eq(i));
            var custWidth = thElem.attr('col-width');

            if (custWidth) {
                expect(thElem.css('width')).toBe(custWidth);
                expect(tdElem.css('width')).toBe(custWidth);
            } else if (tdElem.attr('class') === 'table-icon') {
                expect(thElem.css('width')).toBe(tableIconTdSize + 'px');
                expect(tdElem.css('width')).toBe(tableIconTdSize + 'px');
            } else {
                expect(thElem.css('width')).toBe(colWidth + 'px');
                expect(tdElem.css('width')).toBe(colWidth + 'px');
            }
        });
    }

    function verifyCallbacks(thElems) {
        expect(scope.sortCallback).not.toHaveBeenCalled();

        // first test header has no 'sortable' attr
        thElems[0].click();
        expect(scope.sortCallback).not.toHaveBeenCalled();

        // the other headers have 'sortable'
        for(var i = 1; i < numTestElems; i += 1) {
            thElems[i].click();
            expect(scope.sortCallback).toHaveBeenCalled();
        }
    }

    function verifyIcons(thElems) {
        var currentTh, div;
        // make sure it has the correct icon after clicking
        thElems[1].click();
        currentTh = angular.element(thElems[1]);
        div = currentTh.find('div');
        expect(div.html()).toBe('<svg class="embeddedIcon" ' +
                                'width="10" height="10" viewBox="0 0 50 50">' +
                                '<g class="icon upArrow">' +
                                '<rect width="50" height="50" rx="5"></rect>' +
                                '<use width="50" height="50" class="glyph" ' +
                                'xmlns:xlink="http://www.w3.org/1999/xlink" ' +
                                'xlink:href="#triangleUp">' +
                                '</use>' +
                                '</g></svg>');
        thElems[1].click();
        div = currentTh.find('div');
        expect(div.html()).toBe('<svg class="embeddedIcon" ' +
                                'width="10" height="10" viewBox="0 0 50 50">' +
                                '<g class="icon downArrow">' +
                                '<rect width="50" height="50" rx="5"></rect>' +
                                '<use width="50" height="50" class="glyph" ' +
                                'xmlns:xlink="http://www.w3.org/1999/xlink" ' +
                                'xlink:href="#triangleDown">' +
                                '</use>' +
                                '</g></svg>');

        thElems[2].click();
        div = currentTh.children();
        // clicked on a new element, so the previous icon should have been deleted
        expect(div.html()).toBeFalsy();

        // the new element should have the ascending icon
        currentTh = angular.element(thElems[2]);
        div = currentTh.children();
        expect(div.html()).toBe('<svg class="embeddedIcon" ' +
                                'width="10" height="10" viewBox="0 0 50 50">' +
                                '<g class="icon upArrow">' +
                                '<rect width="50" height="50" rx="5"></rect>' +
                                '<use width="50" height="50" class="glyph" ' +
                                'xmlns:xlink="http://www.w3.org/1999/xlink" ' +
                                'xlink:href="#triangleUp">' +
                                '</use>' +
                                '</g></svg>');
    }

    it('should affirm that onos-fixed-header is working', function () {
        table = angular.element(onosFixedHeaderTags);

        compileTable();
        verifyGivenTags('onos-fixed-header');

        // table will not be fixed unless it receives the 'LastElement' event
        scope.$emit('LastElement');
        scope.$digest();

        verifyCssDisplay();
        verifyColWidth();
    });

    it('should affirm that onos-sortable-header is working', function () {
        var thElems;
        table = angular.element(onosSortableHeaderTags);

        compileTable();
        verifyGivenTags('onos-sortable-header');
        // ctrlCallback functionality is tested in device-spec
        // only checking that it has been called correctly in the directive
        scope.sortCallback = jasmine.createSpy('sortCallback');

        thElems = thead.find('th');
        verifyCallbacks(thElems);
        verifyIcons(thElems);
    });

});
