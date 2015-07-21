// js for sample app view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, mast, ps, wss;

    // internal state
    var selRow,
        detailsPanel,
        pStartY, pHeight,
        wSize;

    // constants
    var topPadding = 20,

        detailsReq = 'sampleDetailsRequest',
        detailsResp = 'sampleDetailsResponse',
        pName = 'item-details-panel',

        propOrder = [ 'id', 'label', 'code'],
        friendlyProps = [ 'Item ID', 'Item Label', 'Special Code' ];


    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.$apply();
    }

    angular.module('ovSample', [])
        .controller('OvSampleCtrl',
        ['$log', '$scope', 'TableBuilderService',
            'FnService', 'WebSocketService',

            function (_$log_, _$scope_, tbs, _fs_, _wss_) {
                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                wss = _wss_;

                var handlers = {};

                $scope.panelData = [];

                function selCb($event, row) {
                    selRow = angular.element($event.currentTarget);
                    if ($scope.selId) {
                        wss.sendEvent(detailsReq, { id: row.id });
                    } else {
                        $log.debug('need to hide details panel');
                        //detailsPanel.hide()
                    }
                    $log.debug('Got a click on:', row);
                }

                tbs.buildTable({
                    scope: $scope,
                    tag: 'sample',
                    selCb: selCb
                });

                // details response handler
                handlers[detailsResp] = respDetailsCb;
                wss.bindHandlers(handlers);

                $scope.$on('$destroy', function () {
                    wss.unbindHandlerse(handlers);
                });

                $log.log('OvSampleCtrl has been created');
            }]);
}());
