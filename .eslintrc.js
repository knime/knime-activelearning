module.exports = {
    /*
    * Use
    * extends: ['./config/.eslintrc-nuxt.js']
    * for nuxt.js projects,
    * extends: ['./config/.eslintrc-vue.js']
    * for Vue.js projects,
    * extends: ['./config/.eslintrc-legacy.js']
    * for legacy views (KNIME AP, ES3), and
    * extends: ['./config/.eslintrc-base.js']
    * for other projects
    */
    extends: ['./config/.eslintrc-legacy.js'],
    overrides: [{
        files: ['*.js'],
        globals: {
            knimeService: false
        }
    }]
};
