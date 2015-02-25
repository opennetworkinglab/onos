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
 ONOS GUI -- Widget -- Button Service
 */
(function () {
    'use strict';

    var $log, fs, is;

    var btnSize = 25;

    function noop() {}

    function createDiv(div, cls, id) {
        return div.append('div')
            .classed(cls, true)
            .attr('id', id);
    }

    function button(div, id, gid, cb, tooltip) {
        if (!div) {
            $log.warn('Button cannot append to div');
            return null;
        }

        var btnDiv = createDiv(div, 'button', id),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(btnDiv, gid, btnSize, true);

        btnDiv.on('click', cbFnc);

        return {
            id: id,
            click: cbFnc,
            el: btnDiv
        }
    }

    function toggle(div, id, gid, initState, cb, tooltip) {
        if (!div) {
            $log.warn('Toggle cannot append to div');
            return null;
        }

        var sel = !!initState,
            togDiv = createDiv(div, 'toggleButton', id),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(togDiv, gid, btnSize, true);
        togDiv.classed('selected', sel);

        function _toggle(b) {
            if (b === undefined) {
                sel = !sel;
            } else {
                sel = !!b;
            }
            cbFnc(sel);
            togDiv.classed('selected', sel);
        }

        togDiv.on('click', _toggle);

        return {
            id: id,
            el: togDiv,
            selected: function () { return sel; },
            toggle: _toggle
        }
    }

    function radioSet(div, id, rset) {
        if (!div) {
            $log.warn('Radio buttons cannot append to div');
            return null;
        }
        if (!fs.isA(rset)) {
            $log.warn('Radio button set is not an array');
            return null;
        }
        if (rset.length === 0) {
            $log.warn('Cannot create radio button set from empty array');
            return null;
        }
        var rDiv = div.append('div').classed('radioSet', true),
            rads = [],
            sel;

        function _selected() {
            var curr = d3.select(this),
                currId = curr.attr('id');

            // I have it going by id's because I couldn't think of a way
            // to get the radio button's index from the div element
                // We could look at the end of the radio button id for its number
                // but I didn't know how to get the end of the string's number
            if (sel !== currId) {
                var selIndex = _getIndex(),
                    currIndex = _getIndex(currId);
                rads[selIndex].el.classed('selected', false);
                curr.classed('selected', true);
                rads[currIndex].cb();
                sel = currId;
            }
        }

        // given the id, will get the index of element
        // without the id, will get the index of sel
        function _getIndex(id) {
            if (!id) {
                for (var i = 0; i < rads.length; i++) {
                    if (rads[i].id === sel) { return i; }
                }
            } else {
                for (var j = 0; j < rads.length; j++) {
                    if (rads[j].id === id) { return j; }
                }
            }
        }

        rset.forEach(function (btn, index) {
            var rid = {id: id + '-' + index},
                rbtn = angular.extend({}, btn, rid),
                istate = (index === 0),
                rBtnDiv = createDiv(rDiv, 'radioButton', rbtn.id);

            if (istate) { rBtnDiv.classed('selected', true); }
            is.loadIcon(rBtnDiv, rbtn.gid, btnSize, true);
            rbtn.el = rBtnDiv;
            rbtn.cb = fs.isF(rbtn.cb) || noop;

            rBtnDiv.on('click', _selected);

            rads.push(rbtn);
        });
        sel = rads[0].id;
        rads[0].cb();

        return {
            rads: rads,
            selected: function (i) {
                if (i === undefined) { _getIndex(); }
                else { _selected(); }
            }
        }
    }

    angular.module('onosWidget')
        .factory('ButtonService', ['$log', 'FnService', 'IconService',
            function (_$log_, _fs_, _is_) {
                $log = _$log_;
                fs = _fs_;
                is = _is_;

                return {
                    button: button,
                    toggle: toggle,
                    radioSet: radioSet
                };
            }]);

}());