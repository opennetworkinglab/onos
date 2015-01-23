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
 ONOS GUI -- Layer -- Panel Service
 */
(function () {
    'use strict';

    var $log;

    var defaultSettings = {
        position: 'TR',
        side: 'right',
        width: 200
    };

    angular.module('onosLayer')
        .factory('PanelService', ['$log', function (_$log_) {
            $log = _$log_;


            function createPanel(opts) {
                var settings = angular.extend({}, defaultSettings, opts);

                function renderPanel() {

                }

                function showPanel() {

                }

                function hidePanel() {

                }

                var api = {
                    render: renderPanel,
                    show: showPanel,
                    hide: hidePanel
                };

                $log.debug('creating panel with settings: ', settings);
                return api;
            }

            return {
                createPanel: createPanel
            };
        }]);

}());
