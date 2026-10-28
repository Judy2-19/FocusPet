// Smoke test: execute card.html's inline script in a Node stub environment, covering init/load/setupCard/animate
// and drag rotation / front zoom / back description expansion to catch scope and runtime errors.
// Usage: node tools/smoke_card.js app/src/main/assets/card3d/card.html
const fs = require('fs');
const vm = require('vm');

function anyObj() {
  const f = function () { return anyObj(); };
  return new Proxy(f, {
    get(t, p) {
      if (p === 'then') return undefined;
      if (p === Symbol.toPrimitive) return () => 0;
      if (p === 'width' || p === 'height') return 10;
      if (p === 'size' || p === 'length') return 8;
      return anyObj();
    },
    set() { return true; },
    apply() { return anyObj(); },
    construct() { return anyObj(); }
  });
}

function fakeEl() {
  const el = {
    width: 0, height: 0, style: {}, textContent: '',
    clientWidth: 800, clientHeight: 600,
    getContext: () => anyObj(),
    toDataURL: () => 'data:image/png;base64,AAAA',
    appendChild: () => {}, setPointerCapture: () => {},
    querySelectorAll: () => [],
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 800, height: 600 }),
    _l: {},
    addEventListener(t, cb) { (el._l[t] = el._l[t] || []).push(cb); },
    removeEventListener() {}
  };
  return el;
}

let rafCb = null, loadHandler = null;
const els = {};
const windowObj = {
  devicePixelRatio: 2, THREE: anyObj(), onerror: null, Android: undefined,
  addEventListener: (t, cb) => { if (t === 'load') loadHandler = cb; },
  removeEventListener: () => {}
};
const documentObj = {
  getElementById: (id) => (els[id] = els[id] || fakeEl()),
  createElement: () => fakeEl(),
  body: fakeEl()
};

const sandbox = {
  window: windowObj, document: documentObj, location: { search: '' },
  performance: { now: () => 0 },
  requestAnimationFrame: (cb) => { rafCb = cb; return 0; }, cancelAnimationFrame: () => {},
  setTimeout: (cb) => { try { cb(); } catch (e) { console.log('FALLBACK ERROR:', e.message); } return 0; },
  Image: function () {
    this.onload = null; this.onerror = null;
    Object.defineProperty(this, 'src', { set() { if (this.onload) this.onload(); }, get() { return ''; } });
  },
  Math, Array, Object, RegExp, Date, Float32Array, decodeURIComponent, console, Symbol
};
sandbox.globalThis = sandbox;

const html = fs.readFileSync(process.argv[2], 'utf8');
const blocks = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]);
const src = blocks.sort((a, b) => b.length - a.length)[0];

vm.createContext(sandbox);
vm.runInContext(src, sandbox, { filename: 'card-inline.js' });
console.log('1) Script parsing/execution OK');

if (!loadHandler) { console.log('!! No load handler captured'); process.exit(1); }
loadHandler();
console.log('2) Load handler OK (initialization and fallback rendering)');

const setSample = (data) => windowObj.setupCard(data);
setSample({ id: 1, name: 'Kitten', rarity: 'LEGENDARY', locked: false, unlockCost: 0, desc: 'A dress-up kitten with a gray coat' });
console.log('3) setupCard(Legendary) OK');

setSample({ id: 9, name: 'Creator', rarity: 'COMMON', locked: true, unlockCost: 2500, desc: 'The ultimate collection reward' });
console.log('4) setupCard(locked) OK (data.locked regression)');

const cv = els['c'];
function fire(type, x, y) {
  (cv._l[type] || []).forEach(cb => cb({ clientX: x, clientY: y, pointerId: 1 }));
}

setSample({ id: 4, name: 'Starlight Beast', rarity: 'RARE', locked: false, unlockCost: 300, desc: 'Appears only late at night' });
fire('pointerdown', 100, 300);
fire('pointermove', 560, 300);
fire('pointerup', 560, 300);
for (let i = 0; i < 60; i++) if (rafCb) rafCb();
console.log('5) Drag rotation, 60 frames OK (unlimited y)');

fire('pointerdown', 300, 300);
fire('pointerup', 300, 300);
for (let i = 0; i < 5; i++) if (rafCb) rafCb();
console.log('6) Tap OK (front zoom / back description)');

for (const r of ['COMMON', 'RARE', 'LEGENDARY']) {
  setSample({ id: 1, name: 'Test', rarity: r, locked: false, unlockCost: 0, desc: 'Sample description' });
  console.log('7) Rarity ' + r + ' build OK');
}

setSample({ id: 1, name: '\u5c0f\u732b\u54aa', language: 'zh-CN', rarity: 'COMMON', locked: false, unlockCost: 0 });
if (vm.runInContext("tr('Common')", sandbox) !== '\u666e\u901a') throw new Error('Chinese rarity translation failed');
if (documentObj.title !== '\u5ba0\u7269\u5361\u724c') throw new Error('Chinese card title failed');
setSample({ id: 1, name: 'Kitten', language: 'en', rarity: 'COMMON', locked: false, unlockCost: 0 });
if (vm.runInContext("tr('Common')", sandbox) !== 'Common') throw new Error('English switch-back failed');
console.log('8) Chinese / English card switching OK');
console.log('=== All passed ✓ ===');
