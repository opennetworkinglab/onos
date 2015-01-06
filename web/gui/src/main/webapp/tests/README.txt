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
