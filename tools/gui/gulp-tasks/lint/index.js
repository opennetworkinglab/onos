import gulp from 'gulp';
import eslint from 'gulp-eslint';
import gulpIf from 'gulp-if';
import path from 'path';

const files = [
    '../../web/gui/src/main/webapp/app/**/*.js'
];

function isFixed(file) {
    // Has ESLint fixed the file contents?
    return file.eslint != null && file.eslint.fixed;
}

const lint = () => {
    return gulp.src(files)
        .pipe(eslint({
            configFile: path.join(__dirname, 'esconfig.json'),
            useEslintrc: false,
            // Automatically fix trivial issues
            // fix: true,
        }))
        .pipe(eslint.format())
        .pipe(gulpIf(isFixed,
            gulp.dest('../../web/gui/src/main/webapp/app')
        ));
};

const tasks = () => {
    gulp.task('lint', () => lint());
};

export default tasks();