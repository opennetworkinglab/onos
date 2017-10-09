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
 ONOS GUI -- Host View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, fs, mast, ps, wss, is, ns, ks, dps;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        wSize;

    // constants
    var topPdg = 28,
        pName = 'host-details-panel',
        detailsReq = 'hostDetailsRequest',
        detailsResp = 'hostDetailsResponse',
        nameChangeReq = 'hostNameChangeRequest',
        nameChangeResp = 'hostNameChangeResponse';

    var keyBindings = {
        esc: [closePanel, 'Close the details panel'],
        _helpFormat: ['esc'],
    };

    function closePanel() {
        if (detailsPanel.isVisible()) {
            $scope.selId = null;
            detailsPanel.hide();
            return true;
        }
        return false;
    }


    function setUpPanel() {

        dps.empty();
        dps.addContainers();
        dps.addCloseButton(closePanel);

        var top = dps.top();

        dps.addHeading('host-icon', true);
        top.append('div').classed('top-content', true);

        top.append('hr');
    }

    function friendlyPropsList(details) {
        return {
            'Host ID': details.id,
            'IP Address': details.ip[0],
            'MAC Address': details.mac,
            'VLAN': details.vlan,
            'Configured': details.configured,
            'Location': details.location,
        };
    }

    function populateTop(tblDiv, details) {
        is.loadEmbeddedIcon(dps.select('.iconDiv'), details._iconid_type, 40);
        dps.top().select('h2').text(details.name);
        dps.addPropsList(tblDiv, friendlyPropsList(details));
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(dps.select('.top-content'), details);
        detailsPanel.height(pHeight);
        // configure width based on content.. for now hardcoded
        detailsPanel.width(400);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.$apply();
    }

    function respNameCb(data) {
        if (data.warn) {
            $log.warn(data.warn, data.id);
            top.select('h2').text(data.id);
        }
    }

    function createDetailsPanel() {
        detailsPanel = dps.create(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
            scope: $scope,
            keyBindings: keyBindings,
            nameChangeRequest: nameChangeReq,
        });

        dps.setResponse(detailsResp, respDetailsCb);

        $scope.hidePanel = function () { detailsPanel.hide(); };
    }


    // Defines the Host View controller...
    angular.module('ovHost', [])
    .controller('OvHostCtrl',
        ['$log', '$scope',
            '$location',
            'TableBuilderService',
            'FnService', 'MastService', 'PanelService', 'WebSocketService',
            'IconService', 'NavService', 'KeyService', 'DetailsPanelService',

        function (_$log_, _$scope_, _$location_,
                  tbs,
                  _fs_, _mast_, _ps_, _wss_,
                  _is_, _ns_, _ks_, _dps_) {

            var params,
                handlers = {};

            $log = _$log_;
            $scope = _$scope_;
            $loc = _$location_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            wss = _wss_;
            is = _is_;
            ns = _ns_;
            ks = _ks_;
            dps = _dps_;

            params = $loc.search();

            $scope.panelData = {};

            // details panel handlers
            handlers[nameChangeResp] = respNameCb;
            wss.bindHandlers(handlers);

            // query for if a certain host needs to be highlighted
            if (params.hasOwnProperty('hostId')) {
                $scope.selId = params['hostId'];
                wss.sendEvent(detailsReq, { id: $scope.selId });
            }

            function selCb($event, row) {
                if ($scope.selId) {
                    wss.sendEvent(detailsReq, { id: row.id });
                } else {
                    $scope.hidePanel();
                }
                $log.debug('Got a click on:', row);
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'host',
                selCb: selCb,
            });

            $scope.nav = function (path) {
                if ($scope.selId) {
                    ns.navTo(path, { hostId: $scope.selId });
                }
            };

            $scope.$on('$destroy', function () {
                dps.destroy();
                wss.unbindHandlers(handlers);
            });

            $log.log('OvHostCtrl has been created');
        }])

    .directive('hostDetailsPanel',
    ['$rootScope', '$window', '$timeout', 'KeyService', 'DetailsPanelService',
    function ($rootScope, $window, $timeout, ks) {
        return function (scope) {
            var unbindWatch;

            function heightCalc() {
                pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                    + mast.mastHeight() + topPdg;
                wSize = fs.windowSize(pStartY);
                pHeight = wSize.height;
            }

            function initPanel() {
                heightCalc();
                createDetailsPanel();
            }

            // Safari has a bug where it renders the fixed-layout table wrong
            // if you ask for the window's size too early
            if (scope.onos.browser === 'safari') {
                $timeout(initPanel);
            } else {
                initPanel();
            }
            // create key bindings to handle panel
            ks.keyBindings(keyBindings);
            ks.gestureNotes([
                ['click', 'Select a row to show device details'],
                ['scroll down', 'See more devices'],
            ]);

            // if the panelData changes
            scope.$watch('panelData', function () {
                if (!fs.isEmptyObject(scope.panelData)) {
                    populateDetails(scope.panelData);
                    detailsPanel.show();
                }
            });

            // if the window size changes
            unbindWatch = $rootScope.$watchCollection(
                function () {
                    return {
                        h: $window.innerHeight,
                        w: $window.innerWidth,
                    };
                }, function () {
                    if (!fs.isEmptyObject(scope.panelData)) {
                        heightCalc();
                        populateDetails(scope.panelData);
                    }
                }
            );

            scope.$on('$destroy', function () {
                unbindWatch();
                ks.unbindKeys();
                ps.destroyPanel(pName);
            });
        };
    }]);
}());
