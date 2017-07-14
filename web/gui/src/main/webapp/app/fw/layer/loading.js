/*
 *  Copyright 2015-present Open Networking Foundation
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
    var $timeout, ts, fs;

    // constants
    var id = 'loading-anim',
        dir = 'data/img/loading/',
        pfx = '/load-',
        nImgs = 16,
        speed = 100,
        waitDelay = 500;

    // internal state
    var div,
        img,
        th,
        idx,
        task,
        wait,
        images = [];

    function dbg() {
        var args = Array.prototype.slice.call(arguments);
        args.unshift('loading');
        fs.debug.apply(this, args);
    }

    function preloadImages() {
        var idx;

        function addImg(th) {
            var img = new Image();
            img.src = fname(idx, th);
            images.push(img);
        }

        dbg('preload images start...');
        for (idx=1; idx<=nImgs; idx++) {
            addImg('light');
            addImg('dark');
        }
        dbg('preload images DONE!', images);
    }

    function fname(i, th) {
        var z = i > 9 ? '' : '0';
        return dir + th + pfx + z + i + '.png';
    }

    function nextFrame() {
        idx = idx === 16 ? 1 : idx + 1;
        img.attr('src', fname(idx, th));
        task = $timeout(nextFrame, speed);
    }

    // start displaying 'loading...' animation (idempotent)
    function startAnim() {
        dbg('start ANIMATION');
        th = ts.theme();
        div = d3.select('#'+id);
        if (div.empty()) {
            div = d3.select('body').append('div').attr('id', id);
            img = div.append('img').attr('src', fname(1, th));
            idx = 1;
            task = $timeout(nextFrame, speed);
        }
    }

    // stop displaying 'loading...' animation (idempotent)
    function stopAnim() {
        dbg('*stop* ANIMATION');
        if (task) {
            $timeout.cancel(task);
            task = null;
        }
        d3.select('#'+id).remove();
    }

    // schedule function to start animation in the future
    function start() {
        dbg('start (schedule)');
        wait = $timeout(startAnim, waitDelay);
    }

    // cancel future start, if any; stop the animation
    function stop() {
        if (wait) {
            dbg('start CANCELED');
            $timeout.cancel(wait);
            wait = null;
        }
        stopAnim();
    }

    // return true if start() has been called but not stop()
    function waiting() {
        return !!wait;
    }

    angular.module('onosLayer')
        .factory('LoadingService',
        ['$timeout', 'ThemeService', 'FnService', 'WebSocketService',

            function (_$timeout_, _ts_, _fs_, wss) {
            $timeout = _$timeout_;
            ts = _ts_;
            fs = _fs_;

            preloadImages();

            var self = {
                start: start,
                stop: stop,
                waiting: waiting,
            };
            wss._setLoadingDelegate(self);
            return self;
        }]);

}());
