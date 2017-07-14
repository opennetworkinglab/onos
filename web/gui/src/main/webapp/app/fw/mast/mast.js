/*
 * Copyright 2014-present Open Networking Foundation
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
 ONOS GUI -- Masthead Module
 */
(function () {
    'use strict';

    // configuration
    var mastHeight = 48,
        padMobile = 16,
        dialogOpts = {
            edge: 'left',
        };

    var ls;

    // In the case of Masthead, we cannot cache the lion bundle, because we
    // call too early (before the lion data is uploaded from the server).
    // So we'll dig into the lion service for each request...
    function getLion(x) {
        var lion = ls.bundle('core.fw.Mast');
        return lion(x);
    }

    angular.module('onosMast', ['onosNav'])
        .controller('MastCtrl',
        ['$log', '$scope', '$location', '$window', 'WebSocketService',
            'NavService', 'DialogService', 'LionService',

        function ($log, $scope, $location, $window, wss, ns, ds, _ls_) {
            var self = this;

            ls = _ls_;

            $scope.lion = getLion;

            function triggerRefresh(action) {
                var uicomp = action === 'add' ? getLion('uicomp_added')
                                              : getLion('uicomp_removed'),
                    okupd = getLion('ui_ok_to_update');

                function createConfirmationText() {
                    var content = ds.createDiv();
                    content.append('p').text(uicomp + ' ' + okupd);
                    return content;
                }

                function dOk() {
                    $log.debug('Refreshing GUI');
                    $window.location.reload();
                }

                function dCancel() {
                    $log.debug('Canceling GUI refresh');
                }

                // NOTE: We use app-dialog (CSS) since we will most likely
                //         invoke this when we (de)activate apps.
                //       However we have added this to the masthead, because
                //         apps could be injected externally (via the onos-app
                //         command) and we might be looking at some other view.
                ds.openDialog('app-dialog', dialogOpts)
                    .setTitle(getLion('confirm_refresh_title'))
                    .addContent(createConfirmationText())
                    .addOk(dOk)
                    .addCancel(dCancel)
                    .bindKeys();
            }

            wss.bindHandlers({
                'guiAdded': function () { triggerRefresh('add'); },
                'guiRemoved': function () { triggerRefresh('rem'); },
            });

            // delegate to NavService
            self.toggleNav = function () {
                ns.toggleNav();
            };

            // onosUser is a global set via the index.html generated source
            $scope.username = function () {
                return onosUser || getLion('unknown_user');
            };

            // The problem with the following is that the localization bundle
            //  hasn't been uploaded from the server at this point, so we get
            //  a lookup miss => '%tt_help%'
            // $scope.helpTip = getLion('tt_help');
            // We would need to figure out how to inject the text later.
            // For now, we'll just leave the tooltip blank.
            $scope.helpTip = '';

            $scope.directTo = function () {
                var curId = $location.path().replace('/', ''),
                    viewMap = $scope.onos['viewMap'],
                    helpUrl = viewMap[curId];
                $window.open(helpUrl);
            };

            $log.log('MastCtrl has been created');
        }])

        // also define a service to allow lookup of mast height.
        .factory('MastService', ['FnService', function (fs) {
            return {
                mastHeight: function () {
                    return fs.isMobile() ? mastHeight + padMobile : mastHeight;
                },
            };
        }]);

}());
