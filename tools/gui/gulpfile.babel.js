import gulp from 'gulp';
import * as Tasks from './gulp-tasks/';

gulp.task('build', ['bundle-css', 'bundle-vendor', 'bundle-js']);
gulp.task('tests', ['test']);

gulp.task('default', function() {
    // Do stuff
});