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

        var btnDiv = createDiv(div, 'btn', id),
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
            togDiv = createDiv(div, 'tog', id),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(togDiv, gid, btnSize, true);

        function _toggle(b) {
            if (b === undefined) {
                sel = !sel;
            } else {
                sel = !!b;
            }
            cbFnc(sel);
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
        var rDiv = div.append('div').classed('rset', true),
            sel = 0,
            rads = [];

        rset.forEach(function (btn, index) {
            var rid = {id: id + '-' + index},
                rbtn = angular.extend({}, btn, rid),
                istate = (index === 0),
                rtog = toggle(rDiv, rbtn.id, rbtn.gid, istate,
                    rbtn.cb, rbtn.tooltip);

            rtog.el = (rtog.el).classed('tog', false).classed('rad', true);
            rads.push(rtog);
        });

        return {
            rads: rads,
            selected: function (i) {
                if (i === undefined) { return sel; }
                else if (i < 0 || i >= rads.length) {
                    $log.error('Cannot select radio button of index ' + i);
                }
                else {
                    if (i !== sel) {
                        rads[sel].toggle(false);
                        rads[i].toggle(true);
                        sel = i;
                    }
                }
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