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

    var btnSize = 30;

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
            svg = btnDiv.append('svg'),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(btnDiv, gid, btnSize);

        btnDiv.on('click', cbFnc);

        return {
            id: id,
            click: cbFnc,
            el: btnDiv
        }
    }

    function toggle(div, id, gid, cb, tooltip) {
        if (!div) {
            $log.warn('Toggle cannot append to div');
            return null;
        }

        var sel = false,
            togDiv = createDiv(div, 'tog', id),
            svg = togDiv.append('svg'),
            cbFnc = fs.isF(cb) || noop;

        is.loadIcon(togDiv, gid, btnSize);

        return {
            id: id,
            el: togDiv,
            selected: function () { return sel; },
            toggle: function (b) {
                if (b === undefined) {
                    sel = !sel;
                } else {
                    sel = !!b;
                }
                cbFnc(sel);
            }
        }
    }

    function radioSet(div, id, rset) {
        return {}
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