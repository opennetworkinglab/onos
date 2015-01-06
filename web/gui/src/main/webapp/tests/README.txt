####
# Unit and integration tests for code under the /app directory
####

To run these tests, karma, node.js etc needs to be installed in the
build environment.

From the karma installation directory, execute the following:

    $ karma start {_path_to_}/src/main/webapp/tests/karma.conf.js

This will launch and capture a browser, install and run the unit tests.

The configuration is currently set to re-run the tests every time a
file change is detected, (i.e. each time a source file is saved).

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
