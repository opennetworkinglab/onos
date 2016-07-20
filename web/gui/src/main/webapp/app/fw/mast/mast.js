/*
 * Copyright 2014-present Open Networking Laboratory
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

    // injected services
    var $log;

    // configuration
    var mastHeight = 48,
        padMobile = 16;

    var dialogId = 'app-dialog',
        dialogOpts = {
            edge: 'left'
        };

        angular.module('onosMast', ['onosNav'])
        .controller('MastCtrl', ['$log', '$scope', '$window', 'WebSocketService', 'NavService',
                                    'DialogService',

        function (_$log_, $scope, $window, wss, ns, ds) {
            var self = this;

            $log = _$log_;

            // initialize mast controller here...
            self.radio = null;

            function triggerRefresh(action) {
                function createConfirmationText() {
                    var content = ds.createDiv();
                    content.append('p').text(action + ' Press OK to update the GUI.');
                    return content;
                }


                function dOk() {
                    $log.debug('Refreshing GUI');
                    $window.location.reload();
                }

                function dCancel() {
                    $log.debug('Canceling GUI refresh');
                }

                ds.openDialog(dialogId, dialogOpts)
                    .setTitle('Confirm GUI Refresh')
                    .addContent(createConfirmationText())
                    .addOk(dOk)
                    .addCancel(dCancel)
                    .bindKeys();
            }

            wss.bindHandlers({
                'guiAdded': function () { triggerRefresh('New GUI components were added.') },
                'guiRemoved': function () { triggerRefresh('Some GUI components were removed.') }
            });

            // delegate to NavService
            self.toggleNav = function () {
                ns.toggleNav();
            };

            $scope.user = onosAuth || '(no one)';

            $log.log('MastCtrl has been created');
        }])

        // also define a service to allow lookup of mast height.
        .factory('MastService', ['FnService', function (fs) {
            return {
                mastHeight: function () {
                    return fs.isMobile() ? mastHeight + padMobile : mastHeight;
                }
            }
        }]);

}());
