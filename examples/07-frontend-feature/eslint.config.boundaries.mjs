// The frontend's boundary enforcer — the lint-time equivalent of the backend's
// ApplicationModulesTest (example 04). `eslint-plugin-boundaries` classifies
// every file into an "element type" and then allows/denies imports between
// them. A feature can be reached only through its `index.ts` barrel; one
// feature can't import another feature's internals; nothing imports `app`.
//
// This is what makes "one feature folder per backend bounded context" a rule
// the build checks, not a convention people remember. A flat-config sketch —
// the real project layers this onto a shared base config.

import boundaries from 'eslint-plugin-boundaries';

export default [
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { boundaries },
    settings: {
      // Classify files into element types by where they live.
      'boundaries/elements': [
        { type: 'shared', pattern: 'src/shared/*' },
        { type: 'feature', pattern: 'src/features/*', capture: ['featureName'] },
        { type: 'app', pattern: 'src/app/*' },
      ],
    },
    rules: {
      'boundaries/no-private': 'error',
      'boundaries/element-types': [
        'error',
        {
          default: 'disallow',
          rules: [
            // The kernel is self-contained: it may use only itself.
            { from: 'shared', allow: ['shared'] },

            // A feature may use the kernel and — crucially — ONLY the public
            // entrypoint of OTHER features (their index.ts), never their
            // internals. It may freely use its own internals.
            {
              from: 'feature',
              allow: [
                'shared',
                ['feature', { featureName: '${from.featureName}' }],
              ],
            },

            // The app composition root may wire anything together.
            { from: 'app', allow: ['shared', 'feature', 'app'] },
          ],
        },
      ],
      // Features must be entered through their barrel, not deep-imported.
      'boundaries/entry-point': [
        'error',
        {
          default: 'disallow',
          rules: [
            { target: ['feature'], allow: 'index.ts' },
            { target: ['shared'], allow: '**' },
          ],
        },
      ],
    },
  },
];
