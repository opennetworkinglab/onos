module.exports = {
    "extends": "google",
    "installedESLint": true,
    "globals": {
        "angular": true,
        "d3": true,
        "_": true
    },
    "rules": {
        "brace-style": 0,
        "no-void": 0,
        "require-jsdoc": 0,
        "padded-blocks": 0,
        "quote-props": 0,
        "no-warning-comments": 0,
        "object-curly-spacing": ["error", "always"],
        "indent": ["error", 4],
        "one-var": 0,
        "space-before-function-paren": ["error", { "anonymous": "always", "named": "never" }]
    }
};
