// ch09-01-time-agoSpec.js

describe('timeAgo Filter', function () {
    beforeEach(module('filterApp'));

    var filter;
    beforeEach(inject(function (timeAgoFilter) {
        filter = timeAgoFilter;
    }));

    it('should respond based on timestamp', function() {
        // The presence of new Date().getTime() makes it slightly
        // hard to unit test deterministically.
        // Ideally, we would inject a dateProvider into the timeAgo
        // filter, but we are trying to keep it simple for now.
        // So, we assume that our tests are fast enough to execute
        // in mere milliseconds.

        var t = new Date().getTime();
        t -= 10000;
        expect(filter(t)).toEqual('seconds ago');
        expect(filter(t, true)).toEqual('minutes ago');

        var fmin = t - 1000 * 60;
        expect(filter(fmin)).toEqual('minutes ago');
        expect(filter(fmin, true)).toEqual('minutes ago');

        var fhour = t - 1000 * 60 * 68;
        expect(filter(fhour)).toEqual('hours ago');
        expect(filter(fhour, true)).toEqual('hours ago');

        var fday = t - 1000 * 60 * 60 * 26;
        expect(filter(fday)).toEqual('days ago');
        expect(filter(fday, true)).toEqual('days ago');

        var fmon = t - 1000 * 60 * 60 * 24 * 34;
        expect(filter(fmon)).toEqual('months ago');
        expect(filter(fmon, true)).toEqual('months ago');
    });
});
