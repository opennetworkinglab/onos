/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

/*
 ONOS GUI -- Lion -- Localization Utilities
 */
(function () {
    'use strict';

    var $log, fs;

    // returns a lion bundle for the given key
    function bundle(key) {

        // TODO: Use message handler mech. to get bundle from server
        // For now, a fake placeholder bundle

        var bun = {
            computer: 'Calcolatore',
            disk: 'Disco',
            monitor: 'Schermo',
            keyboard: 'Tastiera'
        };

        $log.warn('Using fake bundle', bun);
        return bun;
    }

    angular.module('onosUtil')
        .factory('LionService', ['$log', 'FnService',

        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            return {
                bundle: bundle
            };
        }]);
}());
