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

    // configuration
    var mastHeight = 48,
        padMobile = 16,
        dialogOpts = {
            edge: 'left'
        },
        msg = {
            add: { adj: 'New', op: 'added'},
            rem: { adj: 'Some', op: 'removed'}
        };

    angular.module('onosMast', ['onosNav'])
        .controller('MastCtrl',
        ['$log', '$scope', '$window', 'WebSocketService', 'NavService',
            'DialogService',

        function ($log, $scope, $window, wss, ns, ds) {
            var self = this;

            function triggerRefresh(action) {

                function createConfirmationText() {
                    var content = ds.createDiv(),
                        txt = msg[action];

                    content.append('p').text(
                        txt.adj + ' GUI components were ' + txt.op +
                        '. Press OK to update the GUI.'
                    );
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
                    .setTitle('Confirm GUI Refresh')
                    .addContent(createConfirmationText())
                    .addOk(dOk)
                    .addCancel(dCancel)
                    .bindKeys();
            }

            wss.bindHandlers({
                'guiAdded': function () { triggerRefresh('add') },
                'guiRemoved': function () { triggerRefresh('rem') }
            });

            // delegate to NavService
            self.toggleNav = function () {
                ns.toggleNav();
            };

            // onosAuth is a global set via the index.html generated source
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
