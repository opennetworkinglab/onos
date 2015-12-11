/*
 *  Copyright 2015 Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Layer -- Loading Service

 Provides a mechanism to start/stop the loading animation, center screen.
 */
(function () {
    'use strict';

    // injected references
    var $log, $timeout, ts;

    // constants
    var id = 'loading-anim',
        dir = 'data/img/loading/',
        pfx = '/load-',
        speed = 100;

    // internal state
    var div,
        img,
        th,
        idx,
        task;

    function fname(i) {
        var z = i > 9 ? '' : '0';
        return dir + th + pfx + z + i + '.png';
    }

    function nextFrame() {
        idx = idx === 16 ? 1 : idx + 1;
        img.attr('src', fname(idx));
        task = $timeout(nextFrame, speed);
    }

    // start displaying 'loading...' animation (idempotent)
    function start() {
        th = ts.theme();
        div = d3.select('#'+id);
        if (div.empty()) {
            div = d3.select('body').append('div').attr('id', id);
            img = div.append('img').attr('src', fname(1));
            idx = 1;
            task = $timeout(nextFrame, speed);
        }
    }

    // stop displaying 'loading...' animation (idempotent)
    function stop() {
        if (task) {
            $timeout.cancel(task);
            task = null;
        }
        d3.select('#'+id).remove();
    }

    angular.module('onosLayer')
        .factory('LoadingService', ['$log', '$timeout', 'ThemeService',
        function (_$log_, _$timeout_, _ts_) {
            $log = _$log_;
            $timeout = _$timeout_;
            ts = _ts_;

            return {
                start: start,
                stop: stop
            };
        }]);

}());