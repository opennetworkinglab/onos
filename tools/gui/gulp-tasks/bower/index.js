import gulp from 'gulp';
import bower from 'gulp-bower';

const bowerTask = () => {
    return bower({
        directory: 'vendor',
        cwd: '../../web/gui/src/main/webapp',
    });
};

const tasks = () => {
    gulp.task('bower', () => bowerTask());
};

export default tasks();