// ch09-01-time-ago.js

angular.module('filterApp', [])
    .filter('timeAgo', [function () {
        var _m = 1000 * 60,
            _h = _m * 60,
            _d = _h * 24,
            _mon = _d * 30;

        return function (ts, ignoreSecs) {
            var showSecs = !ignoreSecs,
                now = new Date().getTime(),
                diff = now - ts;

            if (diff < _m && showSecs) {
                return 'seconds ago';
            } else if (diff < _h) {
                return 'minutes ago';
            } else if (diff < _d) {
                return 'hours ago';
            } else if (diff < _mon) {
                return 'days ago';
            } else {
                return 'months ago';
            }
        }
    }]);
