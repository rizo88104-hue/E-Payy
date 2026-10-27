function load(key, def) {
  try { var v = JSON.parse(localStorage.getItem(key)); return v && typeof v === 'object' ? v : def; } catch (e) { return def; }
}
function store(key, val) { try { localStorage.setItem(key, JSON.stringify(val)); } catch (e) {} }

function cookie(name) {
  var parts = document.cookie ? document.cookie.split('; ') : [];
  for (var i = 0; i < parts.length; i++) {
    var eq = parts[i].indexOf('=');
    if (parts[i].slice(0, eq) === name) return decodeURIComponent(parts[i].slice(eq + 1));
  }
  return '';
}

function ApiError(message, status, data) {
  var e = new Error(message);
  e.status = status;
  e.data = data;
  return e;
}

async function api(url, options) {
  options = Object.assign({ credentials: 'same-origin' }, options || {});
  var method = (options.method || 'GET').toUpperCase();
  var headers = Object.assign({ Accept: 'application/json' }, options.headers || {});
  if (method !== 'GET' && method !== 'HEAD') {
    var token = cookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  options.headers = headers;
  var res;
  try {
    res = await fetch(url, options);
  } catch (err) {
    if (err && err.name === 'AbortError') throw err;
    throw ApiError(t('net.err'), 0, null);
  }
  var data = res.status === 204 ? null : await res.json().catch(function () { return null; });
  if (!res.ok) {
    var msg = data && typeof data.detail === 'string' && data.detail ? data.detail : t('err');
    var e = ApiError(msg, res.status, data);
    e.retryAfter = Number(res.headers.get('Retry-After')) || 0;
    throw e;
  }
  return data;
}

function esc(s) {
  return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function safeImg(url) {
  return typeof url === 'string' && /^\/(images|uploads)\/[A-Za-z0-9._\/-]+$/.test(url) && url.indexOf('..') < 0 ? url : '';
}
function safeHttps(url) {
  return typeof url === 'string' && /^https:\/\/[^\s"'<>]+$/i.test(url) ? url : '';
}
function money(n) {
  var v = Math.round(Number(n) * 100) / 100;
  return '$' + v.toLocaleString('en-US', { minimumFractionDigits: v % 1 ? 2 : 0, maximumFractionDigits: 2 });
}
function priceHTML(n) { return '<span class="num">' + money(n) + '</span>'; }
function sum(n) { return Math.round(Number(n)).toLocaleString('en-US').replace(/,/g, ' '); }
function uzsOf(usd, rate) { return rate ? Math.round(Number(usd) * Number(rate)) : 0; }

function specs(p) {
  if (!p._specs) {
    p._specs = {};
    (p.specs || '').split('\n').forEach(function (line) {
      var i = line.indexOf(':');
      if (i > 0) p._specs[line.slice(0, i).trim()] = line.slice(i + 1).trim();
    });
  }
  return p._specs;
}

var ILL = [
  [/protsessor|cpu/i, 'p-cpu'], [/ona plata|anakart|motherboard/i, 'p-mb'], [/xotira|ram/i, 'p-ram'],
  [/video|gpu/i, 'p-gpu'], [/ssd|disk/i, 'p-ssd'], [/hdd/i, 'p-hdd'], [/quvvat|psu/i, 'p-psu'],
  [/korpus|case/i, 'p-case'], [/sovut|cool/i, 'p-cool'], [/monitor/i, 'p-mon'], [/noutbuk|laptop/i, 'p-lap'],
  [/server/i, 'p-case'], [/tarmoq|network/i, 'p-mb'], [/dastur|software|xavfsiz|security/i, 'p-cpu']
];
function illFor(catName) {
  for (var i = 0; i < ILL.length; i++) if (ILL[i][0].test(catName || '')) return ILL[i][1];
  return 'p-acc';
}
function catNameOf(p) { return p.categoryName || (p.category && p.category.name) || ''; }
function catIdOf(p) { return p.categoryId || (p.category && p.category.id) || null; }
function brandNameOf(p) { return p.brandName || (p.brand && p.brand.name) || ''; }
function mainImage(p) {
  if (p.images && p.images.length) return safeImg(p.images[0].url);
  return safeImg(p.imageUrl);
}
function ill(p) { return illFor(catNameOf(p)); }

function photo(p, svgCls) {
  var svg = '<svg class="ill ' + svgCls + '" viewBox="0 0 64 64" aria-hidden="true"><use href="#' + ill(p) + '"/></svg>';
  var src = mainImage(p);
  if (!src) return svg;
  return '<img class="pimg" src="' + esc(src) + '" alt="' + esc(p.name) + '" loading="lazy" decoding="async">' + svg;
}
document.addEventListener('error', function (e) {
  var img = e.target;
  if (!img || img.tagName !== 'IMG' || !img.classList.contains('pimg')) return;
  if (img.parentNode) img.parentNode.classList.remove('has-img');
  img.remove();
}, true);

function stockBadge() { return ''; }

var MAX_QTY = 10;
function cartAdd(cart, p) {
  var row = cart[p.id] || { id: p.id, name: p.name, price: Number(p.price), qty: 0, warranty: 0 };
  if (row.qty >= MAX_QTY) return false;
  row.qty += 1;
  cart[p.id] = row;
  return true;
}
function cartCount(cart) {
  return Object.keys(cart).reduce(function (s, k) { return s + (cart[k] && cart[k].qty > 0 ? cart[k].qty : 0); }, 0);
}

var WARRANTY = [{ years: 1, percent: 0 }];
var STORE = { usdRate: 0, paymentMethods: ['CASH'] };
function setWarranty(list) {
  if (list && list.length) WARRANTY = list.slice().sort(function (a, b) { return a.years - b.years; });
}
function warrantyYears(y) {
  return WARRANTY.some(function (w) { return w.years === Number(y); }) ? Number(y) : WARRANTY[0].years;
}
function warrantyRates() {
  return WARRANTY.filter(function (w) { return w.percent; })
    .map(function (w) { return tYears(w.years) + ' +' + w.percent + '%'; }).join(', ');
}
function warrantyPrice(price, years) {
  var w = WARRANTY.filter(function (x) { return x.years === warrantyYears(years); })[0];
  return Math.round(Number(price) * (100 + (w ? w.percent : 0))) / 100;
}
function loadStorefront() {
  return api('/api/storefront/config').then(function (c) {
    setWarranty(c.warranty);
    STORE.usdRate = Number(c.usdRate) || 0;
    STORE.paymentMethods = c.paymentMethods && c.paymentMethods.length ? c.paymentMethods : ['CASH'];
    return STORE;
  });
}

var PAYMENT_LABELS = { CASH: 'pay.cash', PAYME: 'Payme', CLICK: 'Click' };
function payName(m) { var k = PAYMENT_LABELS[m]; return k && k.indexOf('pay.') === 0 ? t(k) : (k || m); }

var REGIONS = [];
function loadRegions() {
  return api('/api/regions').then(function (list) { REGIONS = list; return list; });
}
function regionName(r) { return r[LANG] || r.uz; }

function phoneDigits(v) { return String(v || '').replace(/\D/g, '').slice(0, 9); }
function phoneMask(v) {
  var d = phoneDigits(v);
  return [d.slice(0, 2), d.slice(2, 5), d.slice(5, 7), d.slice(7, 9)].filter(Boolean).join(' ');
}
