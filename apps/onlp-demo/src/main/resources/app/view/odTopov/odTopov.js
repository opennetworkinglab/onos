/*
 * Copyright 2015-present Open Networking Foundation
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
 Module containing the "business logic" for the layout topology overlay.
 */

(function () {
    'use strict';

    // injected refs
    var $log, flash, wss;

    function doFoo(type, description) {
        flash.flash(description);
        wss.sendEvent('doFoo', {
            type: type
        });
    }

    function clear() {
        // Nothing to do?
    }

    angular.module('ovOdTopov', [])
        .factory('OnlpDemoTopovService',
        ['$log', 'FlashService', 'WebSocketService',

        function (_$log_, _flash_, _wss_) {
            $log = _$log_;
            flash = _flash_;
            wss = _wss_;

            return {
                doFoo: doFoo,
                clear: clear
            };
        }]);
}());
