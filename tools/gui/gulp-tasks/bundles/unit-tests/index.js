import gulp from 'gulp';
import istanbul from 'gulp-istanbul';
import path from 'path';
import { Server } from 'karma';

const tests = ['../../web/gui/src/main/webapp/tests/**/*.js'];

const test = () => {
    new Server({
        configFile: path.join(__dirname, '../../../../../', '/web/gui/src/main/webapp/tests/karma.conf.js'),
        singleRun: true
    }, () => { console.log('done') }).start();
};

const tasks = () => {
    gulp.task('pre-test', () => preTest());
    gulp.task('test', () => test());
};

export default tasks();