/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Cluster View Module
 */

(function () {
    'use strict';

        // injected references
        var $log, $scope, $location, fs, tbs, ns, mast, ps, wss, is, ks;

        // internal state
        var detailsPanel,
            pStartY,
            pHeight,
            top,
            topContent,
            bottom,
            iconDiv,
            nameDiv,
            wSize;


        // constants
        var topPdg = 28,
            ctnrPdg = 24,
            scrollSize = 17,
            portsTblPdg = 50,
            htPdg = 479,
            wtPdg = 532,

            pName = 'node-details-panel',
            detailsReq = 'nodeDetailsRequest',
            detailsResp = 'nodeDetailsResponse';

        function closePanel() {
            if (detailsPanel.isVisible()) {
                $scope.selId = null;
                detailsPanel.hide();
                return true;
            }
            return false;
        }

        function addCloseBtn(div) {
            is.loadEmbeddedIcon(div, 'close', 20);
            div.on('click', closePanel);
        }

        function setUpPanel() {
            var container, closeBtn, tblDiv;
            detailsPanel.empty();

            container = detailsPanel.append('div').classed('container', true);

            top = container.append('div').classed('top', true);
            closeBtn = top.append('div').classed('close-btn', true);
            addCloseBtn(closeBtn);
            iconDiv = top.append('div').classed('dev-icon', true);
            top.append('h2');
            topContent = top.append('div').classed('top-content', true);
            top.append('hr');

            //ToDo add more details
        }

        function populateTop(details) {
            is.loadEmbeddedIcon(iconDiv, 'm_node', 40);
            top.select('h2').html(details.id);

            //ToDo : Add more details
        }

        function createDetailsPane() {
            detailsPanel = ps.createPanel(pName, {
                width: wSize.width,
                margin: 0,
                hideMargin: 0
            });
            detailsPanel.el().style({
                position: 'absolute',
                top: pStartY + 'px'
            });
            $scope.hidePanel = function () { detailsPanel.hide(); };
            detailsPanel.hide();
        }

        function populateDetails(details) {

            setUpPanel();
            populateTop(details);

            //ToDo add more details
            detailsPanel.height(pHeight);
            detailsPanel.width(wtPdg);

            //Todo : remove this when server implementation is done
            detailsPanel.show();
        }

        function respDetailsCb(data) {
            $scope.panelData = data.details;
            $scope.$apply();
            //TODO Use populateDetails() when merging server code
            $log.debug('Get the details:', $scope.panelData.id);
        }


    angular.module('ovCluster', [])
        .controller('OvClusterCtrl',
        ['$log', '$scope', 'TableBuilderService', 'NavService', 'MastService',
        'PanelService', 'KeyService', 'IconService','WebSocketService',
        'FnService',

            function (_$log_, _$scope_, tbs, _ns_, _mast_, _ps_, _ks_, _is_,
                    _wss_, _fs_) {
                var params,
                    handlers = {};

                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                ns = _ns_;
                is = _is_;
                wss = _wss_;
                mast = _mast_;
                ps = _ps_;

                $scope.panelData = {};

                tbs.buildTable({
                    scope: $scope,
                    selCb: selCb,
                    tag: 'cluster'
                });

            // details panel handlers
            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            function selCb($event, row) {
                if ($scope.selId) {
                    wss.sendEvent(detailsReq, {id: row.id});

                    //ToDo : Remove this line when server implmentation is complete
                    populateDetails($scope.selId);
                } else {
                    $scope.hidePanel();
                }
                $log.debug('Got a click on:', row);
            }

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

                $log.log('OvClusterCtrl has been created');
            }])


    .directive('nodeDetailsPanel',
    ['$rootScope', '$window', '$timeout', 'KeyService',
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
                createDetailsPane();
            }

            // Safari has a bug where it renders the fixed-layout table wrong
            // if you ask for the window's size too early
            if (scope.onos.browser === 'safari') {
                $timeout(initPanel);
            } else {
                initPanel();
            }

            // if the window size changes
            unbindWatch = $rootScope.$watchCollection(
                function () {
                    return {
                        h: $window.innerHeight,
                        w: $window.innerWidth
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
                ps.destroyPanel(pName);
            });
        };
    }]);
}());
