/*
 * Copyright 2017-present Open Networking Foundation
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
 ONOS GUI -- Widget -- Table Detail Panel Service
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs;

    // constants
    // var refreshInterval = 2000;

    /*
     * NOTE: work-in-progress; this relates to ONOS-5579 (currently stalled)
     */

    function noop() {}

    // TODO: describe the input object for the main function
    // example params to (functionX):
    // {
    //    ...
    // }
    function buildBasePanel(opts) {
        var popTopF = fs.isF(opts.popTop) || noop,
            popMidF = fs.isF(opts.popMid) || noop,
            popBotF = fs.isF(opts.popBot) || noop;

        $log.debug('options are', opts);

        // TODO use panel service to create base panel

        // TODO: create divs, and pass into invocations of popTopF(div), etc.
    }

    // more functions

    // TODO: add ref to PanelService
    angular.module('onosWidget')
    .factory('TableDetailService',
    ['$log', 'FnService',

    function (_$log_, _fs_) {
        $log = _$log_;
        fs = _fs_;

        return {
            buildBasePanel: buildBasePanel,
        };
    }]);
}());
