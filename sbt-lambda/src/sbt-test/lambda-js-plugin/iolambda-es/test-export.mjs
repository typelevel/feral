if (typeof (await import('./target/scala-2.13/npm-package/index.js')).mySimpleHandler === 'function') {
  process.exit(0);
} else {
  process.exit(1);
}
