import gulp from 'gulp';
import path from 'path';
import { Server } from 'karma';

const tests = ['../../web/gui/src/main/webapp/tests/**/*.js'];

const test = () => {
    new Server({
        configFile: path.join(__dirname, '../../../../', '/web/gui/src/main/webapp/tests/karma.conf.js'),
    }).start();
};

const tasks = () => {
    gulp.task('test', () => test());
};

export default tasks();