// js for driver view
(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss, mast;

    // constants
    var detailsReq = 'driverDataRequest',
        detailsResp = 'driverDataResponse',
        topPad = 13,
        labelFudge = 14;

    // d3 selections
    var tabular, dMatrix, tabHdRot, tabGrid, first;

    function fixSizes() {
        var dy = fs.noPxStyle(tabular, 'height') +
                fs.noPxStyle(tabHdRot, 'height') + mast.mastHeight() + topPad,
            tHeight = fs.windowSize(dy).height + 'px',
            rowHdr = tabGrid.select('.row-header'),
            w;

        tabGrid.style('height', tHeight);
        if (!rowHdr.empty()) {
            w = fs.noPxStyle(rowHdr, 'width') + labelFudge;
            first.style('width', w + 'px');
        }
    }

    function respDetailsCb(data) {
        $scope.behaviours = data.behaviours;
        $scope.drivers = data.drivers;
        $scope.matrix = data.matrix;
        $scope.$apply();
        fixSizes();
    }

    angular.module('ovDriverMatrix', [])
        .run(['IconService', function (is) {
            // Create our icon-to-glyph binding here:
            is.registerIconMapping('nav_drivers', 'cog');
        }])
        .controller('OvDriverMatrixCtrl',
            ['$rootScope', '$window', '$log', '$scope', '$sce',
                'FnService', 'WebSocketService', 'MastService',

        function ($rootScope, $window, _$log_, _$scope_, $sce,
                  _fs_, _wss_, _mast_) {
            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            wss = _wss_;
            mast = _mast_;

            var handlers = {},
                unbindWatch;

            tabular = d3.select('.tabular-header');
            dMatrix = d3.select('.driver-matrix');
            tabHdRot = d3.select('.table-header-rotated');
            tabGrid = d3.select('.table-grid');
            first = tabHdRot.select('.first');

            unbindWatch = $rootScope.$watchCollection(
                function () {
                    return {
                        h: $window.innerHeight,
                        w: $window.innerWidth
                    };
                }, fixSizes
            );

            $scope.behaviours = [];
            $scope.drivers = [];
            $scope.matrix = {};

            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            wss.sendEvent(detailsReq);

            function cellHit(d, b) {
                var drec = $scope.matrix[d],
                    brec = drec && drec[b];
                return !!brec;
            }

            $scope.cellMarked = cellHit;
            $scope.checkmark = $sce.trustAsHtml("&check;");

            // cleanup
            $scope.$on('$destroy', function () {
                unbindWatch();
                wss.unbindHandlers(handlers);
                $log.log('OvDriverMatrixCtrl has been destroyed');
            });

            $log.log('OvDriverMatrixCtrl has been created');
        }]);
}());
