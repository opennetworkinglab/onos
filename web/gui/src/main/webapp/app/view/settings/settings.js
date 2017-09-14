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
 ONOS GUI -- Component Settings View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, ks, ps, is;

    // internal state
    var detailsPanel,
        panelData,
        top,
        pStartY,
        pHeight,
        wSize = false;

    // constants
    var pName = 'settings-details-panel',
        panelWidth = 540,
        topPdg = 60,
        propOrder = ['fqComponent', 'prop', 'type', 'value', 'defValue', 'desc'],
        friendlyProps = [
            'Component', 'Property', 'Type', 'Value', 'Default',
            'Description',
        ];

    function createDetailsPanel() {
        detailsPanel = ps.createPanel(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
        });

        detailsPanel.el().style({
            position: 'absolute',
            top: pStartY + 'px',
        });

        detailsPanel.hide();
    }

    function closePanel() {
        if (detailsPanel.isVisible()) {
            $scope.selId = null;
            panelData = null;
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
        var container, closeBtn, div;

        detailsPanel.empty();
        detailsPanel.width(panelWidth);

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);

        div = top.append('div').classed('top-content', true);

        function ndiv(cls, addTable) {
            var d = div.append('div').classed(cls, true);
            if (addTable) {
                d.append('table');
            }
        }

        ndiv('settings-title-1');
        ndiv('settings-title-2');
        ndiv('settings-props', true);
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }

        addCell('label', friendlyProps[index] + ':');
        addCell('value', value);
    }

    function populateTop(details) {
        var propsBody = top.select('.settings-props table').append('tbody');

        top.select('.settings-title-1').text(details.component);
        top.select('.settings-title-2').text(details.prop);

        propOrder.forEach(function (prop, i) {
            addProp(propsBody, i, details[prop]);
        });
    }

    function populateDetails() {
        setUpPanel();
        populateTop(panelData);
        detailsPanel.height(pHeight);
    }

    angular.module('ovSettings', [])
        .controller('OvSettingsCtrl',
            ['$log', '$scope',
            'FnService', 'KeyService', 'PanelService',
            'IconService', 'TableBuilderService',

        function (_$log_, _$scope_, _fs_, _ks_, _ps_, _is_, tbs) {
            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            ks = _ks_;
            ps = _ps_;
            is = _is_;
            $scope.panelData = {};

            function selCb($event, row) {
                if ($scope.selId) {
                    panelData = row;
                    populateDetails();
                    detailsPanel.show();
                } else {
                    panelData = null;
                    detailsPanel.hide();
                }
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'setting',
                selCb: selCb,
            });

            ks.keyBindings({
                esc: [$scope.selectCallback, 'Deselect property'],
                _helpFormat: ['esc'],
            });
            ks.gestureNotes([
                ['click row', 'Select / deselect settings property'],
                ['scroll down', 'See more settings'],
            ]);

            $scope.$on('$destroy', function () {
                ks.unbindKeys();
            });

            $log.log('OvSettingsCtrl has been created');
        }])

        .directive('settingsDetailsPanel',
            ['$rootScope', '$window', '$timeout', 'KeyService',
            function ($rootScope, $window, $timeout, ks) {
                return function (scope) {
                    var unbindWatch;

                    function heightCalc() {
                        pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                            + topPdg;
                        wSize = fs.windowSize(pStartY);
                        pHeight = wSize.height;
                    }

                    function initPanel() {
                        heightCalc();
                        createDetailsPanel();
                        $log.debug('start to initialize panel!');
                    }

                    // Safari has a bug where it renders the fixed-layout table wrong
                    // if you ask for the window's size too early
                    if (scope.onos.browser === 'safari') {
                        $timeout(initPanel);
                    } else {
                        initPanel();
                    }
                    // create key bindings to handle panel
                    ks.keyBindings({
                        esc: [closePanel, 'Close the details panel'],
                        _helpFormat: ['esc'],
                    });
                    ks.gestureNotes([
                        ['click', 'Select a row to show property details'],
                        ['scroll down', 'See more properties'],
                    ]);

                    // if the window size changes
                    unbindWatch = $rootScope.$watchCollection(
                        function () {
                            return {
                                h: $window.innerHeight,
                                w: $window.innerWidth,
                            };
                        }, function () {
                            if (panelData) {
                                heightCalc();
                                populateDetails();
                            }
                        }
                    );

                    scope.$on('$destroy', function () {
                        panelData = null;
                        unbindWatch();
                        ks.unbindKeys();
                        ps.destroyPanel(pName);
                    });
                };
            }]);
}());
