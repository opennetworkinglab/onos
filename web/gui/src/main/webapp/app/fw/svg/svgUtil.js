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
 ONOS GUI -- SVG -- Util Service
 */

/*
 The SVG Util Service provides a miscellany of utility functions.
 */

(function () {
    'use strict';

    // injected references
    var $log, fs;

    angular.module('onosSvg')
        .factory('SvgUtilService', ['$log', 'FnService',
        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            function createDragBehavior() {
                $log.warn('SvgUtilService: createDragBehavior -- To Be Implemented');
            }

            function loadGlow() {
                $log.warn('SvgUtilService: loadGlow -- To Be Implemented');
            }

            function cat7() {
                $log.warn('SvgUtilService: cat7 -- To Be Implemented');
            }

            return {
                createDragBehavior: createDragBehavior,
                loadGlow: loadGlow,
                cat7: cat7
            };
        }]);
}());
