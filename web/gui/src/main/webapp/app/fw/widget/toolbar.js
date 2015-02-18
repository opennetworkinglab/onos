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
 ONOS GUI -- Widget -- Toolbar Service
 */
(function () {
    'use strict';

    var $log;

    // rids will hold all the ids used with makeRadio so that you can create
    // more radio buttons not in order
    // TODO: implement this --^
    var rids = {},
        ridCtr = 0;

    // toggle state is used in createToolbar (not needed in makeToggle) (??)
    var toggle = false;

    function makeButton(id, gid, cb) {
        return {
            t: 'btn',
            id: id,
            gid: gid,
            cb: cb
        };
    }

    function makeToggle(id, gid, cb) {
        return {
            t: 'tog',
            id: id,
            gid: gid,
            cb: cb
        };
    }

    function makeRadio(id, cb) {
        return {
            t: 'rad',
            id: id + '-' + ridCtr,
            cb: cb
        };
    }

    function separator() {
        return {
            t: 'sep'
        };
    }

    function createToolbar() {

    }

    angular.module('onosWidget')
        .factory('ToolbarService', ['$log', function (_$log_) {
            $log = _$log_;

            return {
                makeButton: makeButton,
                makeToggle: makeToggle,
                makeRadio: makeRadio,
                separator: separator,
                createToolbar: createToolbar
            };
        }]);

}());