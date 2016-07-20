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
 ONOS GUI -- Widget -- Button Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs, is, tts;

    // configuration
    var btnSize = 25,
        btnPadding = 4;


    // === Helper Functions

    function divExists(div, msg) {
        if (!div) {
            $log.warn('div undefined (' + msg + ')');
        }
        return !!div;
    }

    function createDiv(div, cls, id) {
        return div.append('div')
            .classed(cls, true)
            .attr('id', id);
    }

    function noop() {}

    function buttonWidth() {
        return btnSize + 2 * btnPadding;
    }

    // === BUTTON =================================================

    // div is where to put the button (d3.selection of a DIV element)
    // id should be globally unique
    // gid is glyph ID (from Glyph Service)
    // cb is callback function on click
    // tooltip is text for tooltip
    function button(div, id, gid, cb, tooltip) {
        if (!divExists(div, 'button')) return null;

        var btnDiv = createDiv(div, 'button', id),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(btnDiv, gid, btnSize, true);
        if (tooltip) { tts.addTooltip(btnDiv, tooltip); }

        btnDiv.on('click', cbFnc);

        return {
            id: id,
            width: buttonWidth
        }
    }


    // === TOGGLE BUTTON ==========================================

    // div is where to put the button (d3.selection of a DIV element)
    // id should be globally unique
    // gid is glyph ID (from Glyph Service)
    // initState is whether the toggle is on or not to begin
    // cb is callback function on click
    // tooltip is text for tooltip
    function toggle(div, id, gid, initState, cb, tooltip) {
        if (!divExists(div, 'toggle button')) return null;

        var sel = !!initState,
            togDiv = createDiv(div, 'toggleButton', id),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(togDiv, gid, btnSize, true);
        togDiv.classed('selected', sel);
        if (tooltip) { tts.addTooltip(togDiv, tooltip); }

        function _toggle(b, nocb) {
            sel = (b === undefined) ? !sel : !!b;
            togDiv.classed('selected', sel);
            nocb || cbFnc(sel);
        }

        // toggle the button state without invoking the callback
        function toggleNoCb() {
            _toggle(undefined, true);
        }

        togDiv.on('click', _toggle);

        return {
            id: id,
            width: buttonWidth,
            selected: function () { return sel; },
            toggle: _toggle,
            toggleNoCb: toggleNoCb
        }
    }


    // === RADIO BUTTON SET =======================================


    // div is where to put the button (d3.selection of a DIV element)
    // id should be globally unique
    // rset is an array of button descriptors of the following form:
    //     {
    //       gid: glyphId,
    //       tooltip: tooltipText,
    //       cb:  callbackFunction
    //     }
    function radioSet(div, id, rset) {
        if (!divExists(div, 'radio button set')) return null;

        if (!fs.isA(rset) || !rset.length) {
            $log.warn('invalid array (radio button set)');
            return null;
        }

        var rDiv = createDiv(div, 'radioSet', id),
            rads = [],
            idxByKey = {},
            currIdx = 0;

        function rsetWidth() {
            return ((btnSize + btnPadding) * rads.length) + btnPadding;
        }

        function rbclick() {
            var id = d3.select(this).attr('id'),
                m = /^.*-(\d+)$/.exec(id),
                idx = Number(m[1]);

            if (idx !== currIdx) {
                rads[currIdx].el.classed('selected', false);
                currIdx = idx;
                rads[currIdx].el.classed('selected', true);
                invokeCurrent();
            }
        }

         // {
         //     gid: gid,
         //     tooltip: ...,       (optional)
         //     key: ...,           (optional)
         //     cb: cb
         //     id: ...             (added by us)
         //     index: ...          (added by us)
         // }

        rset.forEach(function (btn, index) {

            if (!fs.isO(btn)) {
                $log.warn('radio button descriptor at index ' + index +
                            ' not an object');
                return;
            }

            var rid = id + '-' + index,
                initSel = (index === 0),
                rbdiv = createDiv(rDiv, 'radioButton', rid);

            rbdiv.classed('selected', initSel);
            rbdiv.on('click', rbclick);
            is.loadIcon(rbdiv, btn.gid, btnSize, true);
            if (btn.tooltip) { tts.addTooltip(rbdiv, btn.tooltip); }
            angular.extend(btn, {
                el: rbdiv,
                id: rid,
                cb: fs.isF(btn.cb) || noop,
                index: index
            });

            if (btn.key) {
                idxByKey[btn.key] = index;
            }

            rads.push(btn);
        });


        function invokeCurrent() {
            var curr = rads[currIdx];
            curr.cb(curr.index, curr.key);
        }

        function selected(x) {
            var curr = rads[currIdx],
                idx;

            if (x === undefined) {
                return curr.key || curr.index;
            } else {
                idx = idxByKey[x];
                if (idx === undefined) {
                    $log.warn('no radio button with key:', x);
                } else {
                    selectedIndex(idx);
                }
            }
        }

        function selectedIndex(x) {
            if (x === undefined) {
                return currIdx;
            } else {
                if (x >= 0 && x < rads.length) {
                    if (currIdx !== x) {
                        rads[currIdx].el.classed('selected', false);
                        currIdx = x;
                        rads[currIdx].el.classed('selected', true);
                        invokeCurrent();
                    } else {
                        $log.warn('current index already selected:', x);
                    }
                } else {
                    $log.warn('invalid radio button index:', x);
                }
            }
        }

        return {
            width: rsetWidth,
            selected: selected,
            selectedIndex: selectedIndex,
            size: function () { return rads.length; }
        }
    }


    angular.module('onosWidget')
    .factory('ButtonService',
        ['$log', 'FnService', 'IconService', 'TooltipService',

        function (_$log_, _fs_, _is_, _tts_) {
            $log = _$log_;
            fs = _fs_;
            is = _is_;
            tts = _tts_;

            return {
                button: button,
                toggle: toggle,
                radioSet: radioSet
            };
        }]);

}());
