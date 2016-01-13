/*
 * Copyright 2015 Open Networking Laboratory
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

    // constants
    var INSTALLED = 'INSTALLED',
        ACTIVE = 'ACTIVE',
        appMgmtReq = 'appManagementRequest',
        fileUploadUrl = 'applications/upload',
        dialogId = 'app-dialog',
        dialogOpts = {
            edge: 'right'
        },
        strongWarning = {
            'org.onosproject.drivers': true
        },
        discouragement = 'Deactivating or uninstalling this component can' +
        ' have serious negative consequences! Do so at your own risk!!';

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', '$http',
        'FnService', 'TableBuilderService', 'WebSocketService', 'UrlFnService',
        'KeyService', 'DialogService',

    function ($log, $scope, $http, fs, tbs, wss, ufs, ks, ds) {
        $scope.ctrlBtnState = {};
        $scope.uploadTip = 'Upload an application (.oar file)';
        $scope.activateTip = 'Activate selected application';
        $scope.deactivateTip = 'Deactivate selected application';
        $scope.uninstallTip = 'Uninstall selected application';

        function selCb($event, row) {
            // $scope.selId is set by code in tableBuilder
            $scope.ctrlBtnState.selection = !!$scope.selId;
            refreshCtrls();
            ds.closeDialog();  // don't want dialog from previous selection
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
                sortCol: 'state',
                sortDir: 'desc'
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
    }]);
}());
