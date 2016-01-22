/*
 * Copyright 2015-2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- App View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, fs, ps, wss, is, ns, ks, is;

    // internal state
    var  detailsPanel,
         pStartY,
         pHeight,
         top,
         middle,
         bottom,
         iconDiv,
         wSize = false;

    // constants
    var INSTALLED = 'INSTALLED',
        ACTIVE = 'ACTIVE',
        appMgmtReq = 'appManagementRequest',
        topPdg = 50,
        ctnrPdg = 24,
        tbWidth = 470,
        scrollSize = 17,
        pName = 'application-details-panel',
        detailsReq = 'appDetailsRequest',
        detailsResp = 'appDetailsResponse',
        fileUploadUrl = 'applications/upload',
        iconUrlPrefix = 'rs/applications/',
        iconUrlSuffix = '/icon',
        dialogId = 'app-dialog',
        dialogOpts = {
            edge: 'right'
        },
        strongWarning = {
            'org.onosproject.drivers': true
        },
        discouragement = 'Deactivating or uninstalling this component can' +
        ' have serious negative consequences! Do so at your own risk!!',
        propOrder = ['id', 'state', 'category', 'version', 'origin', 'role', 'url'],
        friendlyProps = ['App ID', 'State', 'Category', 'Version', 'Origin', 'Role', 'URL'];

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

    function closePanel() {
        if (detailsPanel.isVisible()) {
            $scope.selId = null;
            detailsPanel.hide();
            return true;
        }
        return false;
    }

    function addCloseBtn(div) {
        is.loadEmbeddedIcon(div, 'plus', 30);
        div.select('g').attr('transform', 'translate(25, 0) rotate(45)');
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

        tblDiv = top.append('div').classed('top-tables', true);
        tblDiv.append('div').classed('left', true).append('table');
        tblDiv.append('div').classed('right', true).append('table');
        tblDiv.append('div').classed('description', true).append('table');

        top.append('hr');

        middle = container.append('div').classed('middle', true);
        tblDiv = middle.append('div').classed('middle-tables', true);
        tblDiv.append('div').classed('readme', true).append('table');

        middle.append('hr');

        bottom = container.append('div').classed('bottom', true);
        tblDiv = bottom.append('div').classed('bottom-tables', true).append('table');
        tblDiv.append('h2').html('Features');
        tblDiv.append('div').classed('features', true).append('table');
        tblDiv.append('h2').html('Required Apps');
        tblDiv.append('div').classed('required-apps', true).append('table');
        tblDiv.append('h2').html('Permissions');
        tblDiv.append('div').classed('permissions', true).append('table');
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function addUrl(tbody, index, value) {
        var href = '<a href="' + value + '" target="_blank">' + value + '</a>';
        addProp(tbody, index, href);
    }

    function addIcon(tbody, value) {
        var tr = tbody.append('tr');
        var td = tr.append('td');
        td.append('img').attr('src', iconUrlPrefix + value + iconUrlSuffix);
    }

    function addContent(tbody, value) {
        var tr = tbody.append('tr');
        tr.append('td').html(value);
    }

    function populateTop(tblDiv, details) {
        var leftTbl = tblDiv.select('.left')
                        .select('table')
                        .append('tbody'),
            rightTbl = tblDiv.select('.right')
                        .select('table')
                        .append('tbody'),
            descriptionTbl = tblDiv.select('.description')
                        .select('table')
                        .append('tbody');

        top.select('h2').html(details.name);

        // place application icon to the left table
        addIcon(leftTbl, details.id);

        // place rest of the fields to the right table
        propOrder.forEach(function (prop, i) {
            var fn = prop === 'url' ? addUrl : addProp;
            fn(rightTbl, i, details[prop]);
        });

        // place description field to the description table
        addContent(descriptionTbl, details.desc);
    }

    function populateMiddle(tblDiv, details) {
        var readmeTbl = tblDiv.select('.readme')
                        .select('table')
                        .append('tbody');

        // place readme field to the readme table
        addContent(readmeTbl, details.readme);
    }

    function populateName(div, name) {
        var lab = div.select('.label'),
            val = div.select('.value');
        lab.html('Friendly Name:');
        val.html(name);
    }

    function populateDetails(details) {
        var nameDiv, topTbs, middleTbs, bottomTbs;
        setUpPanel();

        nameDiv = top.select('.name-div');
        topTbs = top.select('.top-tables');
        middleTbs = middle.select('.middle-tables');
        bottomTbs = bottom.select('.bottom-tables');

        populateName(nameDiv, details.name);
        populateTop(topTbs, details);
        populateMiddle(middleTbs, details);
        populateBottom(bottomTbs, details);

        detailsPanel.height(pHeight);
    }

    function addItem(tbody, item) {
        var tr = tbody.append('tr').attr('width', tbWidth + 'px');
        tr.append('td').attr('width', tbWidth + 'px')
                       .attr('style', 'text-align:left').html(item);
    }

    function addItems(table, items) {
        var tbody = table.append('tbody');
        items.forEach(function (item) {
            addItem(tbody, item);
        });
    }

    function populateBottom(tblDiv, details) {
        var featuresTbl = tblDiv.select('.features')
                                    .select('table'),
            permissionsTbl = tblDiv.select('.permissions')
                                    .select('table'),
            requiredAppsTbl = tblDiv.select('.required-apps')
                                    .select('table');

        addItems(featuresTbl, details.features);
        addItems(requiredAppsTbl, details._required_apps);
        addItems(permissionsTbl, details.permissions);

        featuresTbl.style({
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block'
        });

        detailsPanel.width(tbWidth + ctnrPdg);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.$apply();
    }

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', '$http',
        'FnService', 'TableBuilderService', 'PanelService', 'WebSocketService',
        'IconService', 'UrlFnService', 'KeyService', 'DialogService',

    function (_$log_, _$scope_, $http, _fs_, tbs, _ps_, _wss_, _is_, ufs, _ks_, ds) {
        $log = _$log_;
        $scope = _$scope_;
        wss = _wss_;
        ks = _ks_;
        fs = _fs_;
        ps = _ps_;
        is = _is_;
        $scope.panelData = {};
        $scope.ctrlBtnState = {};
        $scope.uploadTip = 'Upload an application (.oar file)';
        $scope.activateTip = 'Activate selected application';
        $scope.deactivateTip = 'Deactivate selected application';
        $scope.uninstallTip = 'Uninstall selected application';

        var handlers = {};

        // details panel handlers
        handlers[detailsResp] = respDetailsCb;
        wss.bindHandlers(handlers);

        function selCb($event, row) {
            // $scope.selId is set by code in tableBuilder
            $scope.ctrlBtnState.selection = !!$scope.selId;
            refreshCtrls();
            ds.closeDialog();  // don't want dialog from previous selection

            if ($scope.selId) {
                wss.sendEvent(detailsReq, { id: row.id });
            } else {
                $scope.hidePanel();
            }
            $log.debug('Got a click on:', row);
        }

        function refreshCtrls() {
            var row, rowIdx;
            if ($scope.ctrlBtnState.selection) {
                rowIdx = fs.find($scope.selId, $scope.tableData);
                row = rowIdx >= 0 ? $scope.tableData[rowIdx] : null;

                $scope.ctrlBtnState.installed = row && row.state === INSTALLED;
                $scope.ctrlBtnState.active = row && row.state === ACTIVE;
            } else {
                $scope.ctrlBtnState.installed = false;
                $scope.ctrlBtnState.active = false;
            }
        }

        tbs.buildTable({
            scope: $scope,
            tag: 'app',
            selCb: selCb,
            respCb: refreshCtrls,
            // pre-populate sort so active apps are at the top of the list
            sortParams: {
                firstCol: 'state',
                firstDir: 'desc',
                secondCol: 'id',
                secondDir: 'asc'
            }
        });

        // TODO: reexamine where keybindings should be - directive or controller?
        ks.keyBindings({
            esc: [$scope.selectCallback, 'Deselect app'],
            _helpFormat: ['esc']
        });
        ks.gestureNotes([
            ['click row', 'Select / deselect app'],
            ['scroll down', 'See more apps']
        ]);

        function createConfirmationText(action, itemId) {
            var content = ds.createDiv();
            content.append('p').text(action + ' ' + itemId);
            if (strongWarning[itemId]) {
                content.append('p').text(discouragement).classed('strong', true);
            }
            return content;
        }

        function confirmAction(action) {
            var itemId = $scope.selId,
                spar = $scope.sortParams;

            function dOk() {
                $log.debug('Initiating', action, 'of', itemId);
                wss.sendEvent(appMgmtReq, {
                    action: action,
                    name: itemId,
                    sortCol: spar.sortCol,
                    sortDir: spar.sortDir
                });
            }

            function dCancel() {
                $log.debug('Canceling', action, 'of', itemId);
            }

            ds.openDialog(dialogId, dialogOpts)
                .setTitle('Confirm Action')
                .addContent(createConfirmationText(action, itemId))
                .addButton('OK', dOk)
                .addButton('Cancel', dCancel);
        }

        $scope.appAction = function (action) {
            if ($scope.ctrlBtnState.selection) {
                confirmAction(action);
            }
        };

        $scope.$on('FileChanged', function () {
            var formData = new FormData();
            if ($scope.appFile) {
                formData.append('file', $scope.appFile);
                $http.post(ufs.rsUrl(fileUploadUrl), formData, {
                    transformRequest: angular.identity,
                    headers: {
                        'Content-Type': undefined
                    }
                })
                    .finally(function () {
                        $scope.sortCallback($scope.sortParams);
                        document.getElementById('inputFileForm').reset();
                    });
            }
        });

        $scope.$on('$destroy', function () {
            ks.unbindKeys();
            wss.unbindHandlers(handlers);
        });

        $log.log('OvAppCtrl has been created');
    }])

    // triggers the input form to appear when button is clicked
    .directive('triggerForm', function () {
        return {
            restrict: 'A',
            link: function (scope, elem) {
                elem.bind('click', function () {
                    document.getElementById('uploadFile')
                        .dispatchEvent(new MouseEvent('click'));
                });
            }
        };
    })

    // binds the model file to the scope in scope.appFile
    // sends upload request to the server
    .directive('fileModel', ['$parse',
            function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, elem, attrs) {
                var model = $parse(attrs.fileModel),
                    modelSetter = model.assign;

                elem.bind('change', function () {
                    scope.$apply(function () {
                        modelSetter(scope, elem[0].files[0]);
                    });
                    scope.$emit('FileChanged');
                });
            }
        };
    }])

    .directive('applicationDetailsPanel',
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
                    createDetailsPane();
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
                    _helpFormat: ['esc']
                });
                ks.gestureNotes([
                    ['click', 'Select a row to show application details'],
                    ['scroll down', 'See more application']
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
                    ks.unbindKeys();
                    ps.destroyPanel(pName);
                });
            };
        }]);
}());
