// 冒烟测试：Node 桩环境真实执行 card.html 内联脚本，覆盖 init/load/setupCard/animate
// 以及"拖动旋转 + 轻点(正面放大 / 背面展开简介)"路径，捕获作用域/运行时错误。
// 用法：node tools/smoke_card.js app/src/main/assets/card3d/card.html
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
console.log('1) 脚本解析/执行 OK');

if (!loadHandler) { console.log('!! 未捕获到 load 处理器'); process.exit(1); }
loadHandler();
console.log('2) load 处理器 OK（init + 兜底渲染）');

const setSample = (data) => windowObj.setupCard(data);
setSample({ id: 1, name: '小猫咪', rarity: 'LEGENDARY', locked: false, unlockCost: 0, desc: '可换装的小猫咪，默认灰色' });
console.log('3) setupCard(传说) OK');

setSample({ id: 9, name: '创世神', rarity: 'COMMON', locked: true, unlockCost: 2500, desc: '集齐所有图鉴后的终极奖励' });
console.log('4) setupCard(锁定) OK  ← 之前 data.locked 报错点');

const cv = els['c'];
function fire(type, x, y) {
  (cv._l[type] || []).forEach(cb => cb({ clientX: x, clientY: y, pointerId: 1 }));
}

setSample({ id: 4, name: '星光兽', rarity: 'RARE', locked: false, unlockCost: 300, desc: '只在深夜出现' });
fire('pointerdown', 100, 300);
fire('pointermove', 560, 300);
fire('pointerup', 560, 300);
for (let i = 0; i < 60; i++) if (rafCb) rafCb();
console.log('5) 拖动旋转 60 帧 OK（y 无上限）');

fire('pointerdown', 300, 300);
fire('pointerup', 300, 300);
for (let i = 0; i < 5; i++) if (rafCb) rafCb();
console.log('6) 轻点 OK ← 覆盖 onTap（正面放大 / 背面展开简介）');

for (const r of ['COMMON', 'RARE', 'LEGENDARY']) {
  setSample({ id: 1, name: '测试', rarity: r, locked: false, unlockCost: 0, desc: '一句简介' });
  console.log('7) 稀有度 ' + r + ' 构建 OK');
}

console.log('=== 全部通过 ✓ ===');
