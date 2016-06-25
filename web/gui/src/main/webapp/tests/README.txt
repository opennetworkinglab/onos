####
# Unit and integration tests for code under the /app directory
####

To run these tests, karma, node.js etc needs to be installed in the
build environment.

Once Node.js is installed from this folder execute:

    $ npm install

And then execute the following:

    $ npm test

This will launch and capture a browser, install and run the unit tests.

To re-run the tests every time a
file change is detected, (i.e. each time a source file is saved) use:

    $ npm run test:dev

----------------------------------------------------------------------
Useful Notes
============

Set a 'breakpoint' with the debugger command:

    it('should define four functions', function () {
        debugger;

        expect(fs.isF(gs.init)).toBeTruthy();
        // ...
    });

Open Developer Tools in the captured Chrome browser, and reload the page.
The debugger will break at the given point, allowing you to inspect context.

----------------------------------------------------------------------

