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
 ONOS GUI -- Intent View Module
 */

(function () {
    'use strict';

    // constants and configuration
    var dialogId = 'remove-intent-dialog',
        dialogOpts = {
            edge: 'right',
        };

    // DOM elements
    var dropdown;

    // injected refs
    var $log, $scope, ns, tov, tts, ds;


    function initScope() {
        $scope.topoTip = 'Show selected intent on topology view';
        $scope.resubmitTip = 'Resubmit selected intent';
        $scope.deactivateTip = 'Remove selected intent';
        $scope.purgeTip = 'Purge selected intent';
        $scope.purgeAllTip = 'Purge withdrawn intents';

        $scope.briefTip = 'Switch to brief view';
        $scope.detailTip = 'Switch to detailed view';

        $scope.brief = true;
        $scope.intentState = 'NA';
        $scope.fired = false;
    }

    // === row selection and response callback functions:

    function selCb($event, row) {
        $log.debug('Got a click on:', row);
        var m = /(\d+)\s:\s(.*)/.exec(row.appId),
            id = m ? m[1] : null,
            name = m ? m[2] : null;

        $scope.intentData = ($scope.selId && m) ? {
                appId: id,
                appName: name,
                key: row.key,
                intentType: row.type,
            } : null;

        $scope.intentState = row.state;
        showDropdown(false);
    }

    function respCb() {
        if ($scope.fired) {
            if ($scope.changedData) {
                $scope.intentState = $scope.changedData.state;
            }
            $scope.fired = false;
        }
    }


    // === show-intent functions

    function showDropdown(b) {
        dropdown.style('display', b ? 'block' : 'none');
    }

    function showIntent() {
        var d = $scope.intentData,
            handlers,
            nh;

        if (!d) {
            // no intent selected - nothing to do
            return;
        }

        function setOvAndNavigate(info) {
            d.overlayId = info.id;
            ns.navTo('topo', d);
        }

        function clickMe(data) {
            showDropdown(false);
            setOvAndNavigate(data);
        }

        function setUpSelection(handlers) {
            dropdown.text(null);

            handlers.forEach(function (data) {
                var div = dropdown.append('div');
                div.classed('overlay-choice', true);
                div.text(data.tt);
                div.on('click', function () {
                    clickMe(data);
                });
            });

            showDropdown(true);
        }

        handlers = tov.overlaysAcceptingIntents(d.intentType);
        nh = handlers.length;

        if (nh === 1) {
            setOvAndNavigate(handlers[0]);

        } else if (nh > 1) {
            // let the user choose which overlay to invoke...
            setUpSelection(handlers);

        } else {
            $log.warn('Sorry - no overlay configured to show',
                      'intents of type', d.intentType);
        }
    }


    // === intent action functionality

    // TODO: refactor to move functions below to here...


    // === intent view controller

    angular.module('ovIntent', [])
        .controller('OvIntentCtrl',
        ['$log', '$scope', 'TableBuilderService', 'NavService',
            'TopoOverlayService', 'TopoTrafficService', 'DialogService',

        function (_$log_, _$scope_, tbs, _ns_, _tov_, _tts_, _ds_) {
            $log = _$log_;
            $scope = _$scope_;
            ns = _ns_;
            tov = _tov_;
            tts = _tts_;
            ds = _ds_;

            initScope();

            dropdown = d3.select('div.show-intent-btn .dropdown');

            // set up scope function references...
            $scope.showIntent = showIntent;

            $scope.canShowIntent = function () {
                var d = $scope.intentData;
                return d && tov.overlaysAcceptingIntents(d.intentType).length > 0;
            };

            // build the table
            tbs.buildTable({
                scope: $scope,
                tag: 'intent',
                selCb: selCb,
                respCb: respCb,
                idKey: 'key',
            });


            // TODO: clean up the following code...

            $scope.isIntentInstalled = function () {
                return $scope.intentState === 'Installed';
            };

            $scope.isIntentWithdrawn = function () {
                return $scope.intentState === 'Withdrawn';
            };

            $scope.isHavingWithdrawn = function () {
                var isWithdrawn = false;
                $scope.tableData.forEach(function (intent) {
                    if (intent.state ==='Withdrawn') {
                        isWithdrawn = true;
                    }
                });
                return isWithdrawn;
            };

            function executeAction(action) {
                var content = ds.createDiv(),
                    bPurge = action === 'purge';

                $scope.intentData.intentPurge = bPurge;

                content.append('p').
                        text('Are you sure you want to '+ action +
                        ' the selected intent?');

                function dOk() {
                    var d = $scope.intentData;
                    $log.debug(d);
                    d && (action === 'resubmit' ? tts.resubmitIntent(d) :
                                    tts.removeIntent(d));
                    $scope.fired = true;
                }

                function dCancel() {
                    ds.closeDialog();
                    $log.debug('Canceling remove-intent action');
                }

                ds.openDialog(dialogId, dialogOpts)
                    .setTitle('Confirm Action')
                    .addContent(content)
                    .addOk(dOk)
                    .addCancel(dCancel)
                    .bindKeys();
            }

            function executeActions(action) {
                 var content = ds.createDiv();

                 content.append('p')
                    .text('Are you sure you want to purge all the withdrawn intents?');

                 function dOk() {
                     tts.removeIntents();
                     $scope.fired = true;
                 }

                 function dCancel() {
                     ds.closeDialog();
                     $log.debug('Canceling remove-intents action');
                 }

                 ds.openDialog(dialogId, dialogOpts)
                 .setTitle('Confirm Action')
                 .addContent(content)
                 .addOk(dOk)
                 .addCancel(dCancel)
                 .bindKeys();
            }

            $scope.deactivateIntent = function () {
                executeAction('withdraw');
            };

            $scope.resubmitIntent = function () {
                executeAction('resubmit');
            };

            $scope.purgeIntent = function () {
                executeAction('purge');
            };

            $scope.briefToggle = function () {
                $scope.brief = !$scope.brief;
            };

            $scope.$on('$destroy', function () {
                ds.closeDialog();
                $log.debug('OvIntentCtrl has been destroyed');
            });

            $scope.purgeIntents = function () {
                executeActions('purgeIntents');
            };

            $log.debug('OvIntentCtrl has been created');
        }]);
}());
