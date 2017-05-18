/*
 * Copyright 2015-present Open Networking Laboratory
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
    var $log, $scope, wss, fs, ks, ps, is;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        middle,
        bottom,
        wSize = false,
        activateImmediately = '';

    // constants
    var INSTALLED = 'INSTALLED',
        ACTIVE = 'ACTIVE',
        appMgmtReq = 'appManagementRequest',
        topPdg = 60,
        panelWidth = 540,
        pName = 'application-details-panel',
        detailsReq = 'appDetailsRequest',
        detailsResp = 'appDetailsResponse',
        fileUploadUrl = 'applications/upload',
        activateOption = '?activate=true',
        iconUrlPrefix = 'rs/applications/',
        iconUrlSuffix = '/icon',
        dialogId = 'app-dialog',
        dialogOpts = {
            edge: 'right',
            width: 400
        },
        strongWarning = {
            'org.onosproject.drivers': true
        },
        discouragement = 'Deactivating or uninstalling this component can' +
        ' have serious negative consequences! <br> = DO SO AT YOUR OWN RISK =',
        propOrder = ['id', 'state', 'category', 'version', 'origin', 'role'],
        friendlyProps = ['App ID', 'State', 'Category', 'Version', 'Origin', 'Role'];
        // note: url is handled separately

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
        is.loadEmbeddedIcon(div, 'close', 26);
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

        function ndiv(cls, tcls) {
            var  d = div.append('div').classed(cls, true);
            if (tcls) {
                d.append('table').classed(tcls, true);
            }
        }

        ndiv('app-title');
        ndiv('left app-icon');
        ndiv('right', 'app-props');
        ndiv('app-url');

        container.append('hr');

        middle = container.append('div').classed('middle', true);
        middle.append('div').classed('app-readme', true);

        container.append('hr');

        // TODO: make bottom container scrollable
        bottom = container.append('div').classed('bottom', true);

        function nTable(hdr, cls) {
            bottom.append('h2').text(hdr);
            bottom.append('div').classed(cls, true).append('table');
        }

        nTable('Features', 'features');
        nTable('Required Apps', 'required-apps');
        nTable('Permissions', 'permissions');
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }

        addCell('label', friendlyProps[index] + ':');
        addCell('value', value);
    }

    function urlize(u) {
        u = fs.sanitize(u);
        return '<a href="' + u + '" target="_blank">' + u + '</a>';
    }

    function addIcon(elem, value) {
        elem.append('img').attr('src', iconUrlPrefix + value + iconUrlSuffix);
    }

    function populateTop(details) {
        var propsBody = top.select('.app-props').append('tbody'),
            url = details.url;

        top.select('.app-title').text(details.title);

        addIcon(top.select('.app-icon'), details.id);

        propOrder.forEach(function (prop, i) {
            addProp(propsBody, i, details[prop]);
        });

        if (url) {
            top.select('.app-url').html(urlize(url));
        }
    }

    function populateMiddle(details) {
        middle.select('.app-readme').text(details.readme);
    }

    function populateBottom(details) {

        function addItems(cls, items) {
            var table = bottom.select('.' + cls).select('table'),
                tbody = table.append('tbody');

            items.forEach(function (item) {
                tbody.append('tr').append('td').text(item);
            });
        }

        addItems('features', details.features);
        addItems('required-apps', details.required_apps);
        addItems('permissions', details.permissions);
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(details);
        populateMiddle(details);
        populateBottom(details);
        detailsPanel.height(pHeight);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.selId = data.details.id;
        $scope.ctrlBtnState.selection = data.details.id;
        $scope.$apply();
    }

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', '$http', '$timeout',
         'WebSocketService', 'FnService', 'KeyService', 'PanelService',
         'IconService', 'UrlFnService', 'DialogService', 'TableBuilderService',

    function (_$log_, _$scope_, $http, $timeout, _wss_, _fs_, _ks_, _ps_, _is_,
              ufs, ds, tbs) {
        $log = _$log_;
        $scope = _$scope_;
        wss = _wss_;
        fs = _fs_;
        ks = _ks_;
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
                secondCol: 'title',
                secondDir: 'asc'
            }
        });

        // TODO: reexamine where keybindings should be - directive or controller?
        ks.keyBindings({
            esc: [$scope.selectCallback, 'Deselect application'],
            _helpFormat: ['esc']
        });
        ks.gestureNotes([
            ['click row', 'Select / deselect application'],
            ['scroll down', 'See more applications']
        ]);

        function createConfirmationText(action, itemId) {
            var content = ds.createDiv();
            content.append('p').text(fs.cap(action) + ' ' + itemId);
            if (strongWarning[itemId]) {
                content.append('p').html(fs.sanitize(discouragement))
                    .classed('strong', true);
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
                if (action == 'uninstall') {
                    detailsPanel.hide();
                } else {
                    wss.sendEvent(detailsReq, {id: itemId});
                }
            }

            function dCancel() {
                $log.debug('Canceling', action, 'of', itemId);
            }

            ds.openDialog(dialogId, dialogOpts)
                .setTitle('Confirm Action')
                .addContent(createConfirmationText(action, itemId))
                .addOk(dOk)
                .addCancel(dCancel)
                .bindKeys();
        }

        $scope.appAction = function (action) {
            if ($scope.ctrlBtnState.selection) {
                confirmAction(action);
            }
        };

        $scope.$on('FileChanged', function () {
            var formData = new FormData(),
                url;

            if ($scope.appFile) {
                formData.append('file', $scope.appFile);
                url = fileUploadUrl + activateImmediately;

                $http.post(ufs.rsUrl(url), formData, {
                    transformRequest: angular.identity,
                    headers: {
                        'Content-Type': undefined
                    }
                })
                .finally(function () {
                    activateImmediately = '';
                    $scope.sortCallback($scope.sortParams);
                    document.getElementById('inputFileForm').reset();
                    $timeout(function () { wss.sendEvent(detailsReq); }, 250);
                });
            }
        });

        $scope.appDropped = function() {
            activateImmediately = activateOption;
            $scope.$emit('FileChanged');
            $scope.appFile = null;
        };

        $scope.$on('$destroy', function () {
            ks.unbindKeys();
            wss.unbindHandlers(handlers);
            ds.closeDialog();
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

    .directive("filedrop", function ($parse, $document) {
        return {
            restrict: "A",
            link: function (scope, element, attrs) {
                var onAppDrop = $parse(attrs.onFileDrop);

                // When an item is dragged over the document
                var onDragOver = function (e) {
                    d3.select('#frame').classed('dropping', true);
                    e.preventDefault();
                };

                // When the user leaves the window, cancels the drag or drops the item
                var onDragEnd = function (e) {
                    d3.select('#frame').classed('dropping', false);
                    e.preventDefault();
                };

                // When a file is dropped
                var loadFile = function (file) {
                    scope.appFile = file;
                    scope.$apply(onAppDrop(scope));
                };

                // Dragging begins on the document
                $document.bind("dragover", onDragOver);

                // Dragging ends on the overlay, which takes the whole window
                element.bind("dragleave", onDragEnd)
                    .bind("drop", function (e) {
                        $log.info('Drag leave', e);
                        loadFile(e.dataTransfer.files[0]);
                        onDragEnd(e);
                    });
            }
        };
    })

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
                    ['scroll down', 'See more applications']
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
