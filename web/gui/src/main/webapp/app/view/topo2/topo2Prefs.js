/*
* Copyright 2016-present Open Networking Foundation
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

(function () {

    // Injected Services
    var ps;

    var defaultPrefsState = {
        insts: 1,
        summary: 1,
        detail: 1,
        hosts: 0,
        offdev: 1,
        dlbls: 0,
        hlbls: 0,
        porthl: 1,
        bg: 0,
        spr: 0,
        ovid: 'traffic', // default to traffic overlay
        toolbar: 0,
    };

    function topo2Prefs() {
        return ps.getPrefs('topo2_prefs', defaultPrefsState);
    }

    function get(key) {
        var preferences = topo2Prefs();
        return preferences[key];
    }

    function set(key, value) {
        var preferences = topo2Prefs();
        preferences[key] = value;
        ps.setPrefs('topo2_prefs', preferences);
        return preferences[key];
    }

    angular.module('ovTopo2')
    .factory('Topo2PrefsService', [
        'PrefsService',
        function (_ps_) {

            ps = _ps_;

            return {
                get: get,
                set: set,
            };
        },
    ]);
})();
