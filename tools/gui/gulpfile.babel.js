import gulp from 'gulp';
import * as Tasks from './gulp-tasks/';

gulp.task('build', ['bundle-css', 'bundle-js']);
gulp.task('tests', ['test']);

gulp.task('default', []);

// TODO: Uncomment once npm and buck issues are resolved.
// gulp.task('default', ['bundle-js', 'serve', 'watch-js']);