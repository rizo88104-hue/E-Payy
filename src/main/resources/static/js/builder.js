(function () {
  var $ = function (s) { return document.querySelector(s); };

  var SLOTS = [
    { k: 'cpu', match: /protsessor|cpu/i },
    { k: 'mb', match: /ona plata|motherboard/i },
    { k: 'ram', match: /xotira|ram/i },
    { k: 'cool', match: /sovut|cool/i, opt: true },
    { k: 'gpu', match: /video|gpu/i, opt: true },
    { k: 'disk', match: /ssd|hdd|disk/i },
    { k: 'case', match: /korpus|case/i, opt: true },
    { k: 'psu', match: /quvvat|psu/i }
  ];
  SLOTS.forEach(function (s) { s.label = t('b.' + s.k); s.hint = t('b.' + s.k + '.hint'); });
  var ORDER = SLOTS.length;
  var COMPAT_KEYS = ['Socket', 'RAM turi', 'Form-faktor'];

  var products = [];
  var build = load('build', {});
  var step = 0;
  var form = { customerName: '', phone: '', address: '', region: '', pay: 'CASH', warranty: 0 };
  var placed = null;
  var ready = false;

  function byId(id) { return products.filter(function (p) { return String(p.id) === String(id); })[0]; }
  function picked(k) { return build[k] && byId(build[k]); }
  function pickedParts() { return SLOTS.map(function (s) { return picked(s.k); }).filter(Boolean); }
  function others(k) { return SLOTS.filter(function (s) { return s.k !== k; }).map(function (s) { return picked(s.k); }).filter(Boolean); }
  function saveBuild() { store('build', build); }

  function watts(v) { return parseInt(v, 10) || 0; }
  function isPsu(p) { return !!specs(p)['Quvvat']; }
  function requiredPower(parts) {
    var total = parts.filter(function (p) { return !isPsu(p); }).reduce(function (s, p) { return s + watts(specs(p)['Quvvat sarfi']); }, 0);
    return Math.round((total + 100) * 1.5);
  }
  function values(v) { return v.split('/').map(function (x) { return x.trim().toUpperCase(); }); }
  function keyOk(a, b, k) {
    var sa = specs(a)[k], sb = specs(b)[k];
    if (!sa || !sb) return true;
    var va = values(sa);
    return values(sb).some(function (x) { return va.indexOf(x) >= 0; });
  }
  function conflict(p, rest) {
    for (var i = 0; i < rest.length; i++) {
      for (var j = 0; j < COMPAT_KEYS.length; j++) {
        var k = COMPAT_KEYS[j];
        if (!keyOk(p, rest[i], k)) return t('b.conflict', { k: tSpec(k), v: specs(p)[k], name: rest[i].name, v2: specs(rest[i])[k] });
      }
    }
    var all = rest.concat(p), psu = all.filter(isPsu)[0];
    if (psu && watts(specs(psu)['Quvvat']) < requiredPower(all)) {
      var need = requiredPower(all);
      return p === psu ? t('b.psuShort', { n: need }) : t('b.psuOther', { name: psu.name, n: need });
    }
    return null;
  }
  function serverCompatCheck() { return Promise.resolve(null); }

  function pick(k, id) {
    if (id) build[k] = id; else delete build[k];
    var kept = id ? [byId(id)] : [], removed = [];
    SLOTS.forEach(function (s) {
      var p = picked(s.k);
      if (!p || s.k === k) return;
      if (!conflict(p, kept)) kept.push(p);
      else { delete build[s.k]; removed.push(s.label); }
    });
    saveBuild();
    if (removed.length) showToast(t('b.removed', { x: removed.join(', ') }));
  }

  function firstOpen() {
    for (var i = 0; i < SLOTS.length; i++) if (!SLOTS[i].opt && !picked(SLOTS[i].k)) return i;
    return ORDER;
  }
  function reachable(i) { return i <= firstOpen(); }
  function hrefFor(i) { return i >= ORDER ? '#/buyurtma' : '#/' + (i + 1); }
  function parseRoute() {
    var h = location.hash || '';
    var want = /^#\/buyurtma/.test(h) ? ORDER : (function () {
      var m = h.match(/^#\/(\d+)/);
      return m ? Math.min(Math.max(parseInt(m[1], 10) - 1, 0), SLOTS.length - 1) : 0;
    })();
    var limit = firstOpen();
    if (want > limit) { history.replaceState(null, '', hrefFor(limit)); return limit; }
    return want;
  }

  var enterDir = '', justPicked = '', shownTotal = 0;

  function render() {
    SLOTS.forEach(function (s) { if (build[s.k] && !byId(build[s.k])) delete build[s.k]; });
    renderSteps();
    if (step >= ORDER) renderOrder(); else renderStep(SLOTS[step]);
    renderSide();
    renderNav();
    animate();
  }

  function animate() {
    if (enterDir) {
      ['#bp-title', '#bp-hint', '#bp-main'].forEach(function (s) {
        var el = $(s);
        el.classList.remove('anim-r', 'anim-l'); void el.offsetWidth;
        el.classList.add('anim-' + enterDir);
      });
      var cur = document.querySelector('.bp-st.cur');
      if (cur) cur.classList.add('enter');
      staggerOnce(document.querySelector('.bp-opts'));
      enterDir = '';
    }
    var to = total();
    document.querySelectorAll('[data-total]').forEach(function (el) { tweenNumber(el, shownTotal, to, money); });
    shownTotal = to;
    justPicked = '';
  }

  function renderSteps() {
    $('#bp-steps').innerHTML = SLOTS.map(function (s, i) {
      var p = picked(s.k), cls = 'bp-st' + (p ? ' done' : '') + (i === step ? ' cur' : '') + (p && s.k === justPicked ? ' just' : '');
      var sub = p ? esc(p.name) : (s.opt && i < step ? t('b.skipped') : s.opt ? t('b.optional') : t('b.pick'));
      var inner = '<span class="bp-st-n">' + (p ? '✓' : '+') + '</span><span class="bp-st-t"><b>' + esc(s.label) + '</b><small>' + sub + '</small></span>';
      return '<li>' + (reachable(i) ? '<a class="' + cls + '" href="' + hrefFor(i) + '"' + (i === step ? ' aria-current="step"' : '') + '>' + inner + '</a>'
        : '<span class="' + cls + ' locked" aria-disabled="true">' + inner + '</span>') + '</li>';
    }).join('')
      + '<li>' + (reachable(ORDER) ? '<a class="bp-st' + (step === ORDER ? ' cur' : '') + '" href="#/buyurtma"' + (step === ORDER ? ' aria-current="step"' : '') + '>'
        : '<span class="bp-st locked" aria-disabled="true">')
      + '<span class="bp-st-n">9</span><span class="bp-st-t"><b>' + t('order') + '</b><small>' + t('b.orderSub') + '</small></span>'
      + (reachable(ORDER) ? '</a>' : '</span>') + '</li>';
    var cur = document.querySelector('.bp-st.cur');
    if (cur && cur.scrollIntoView) cur.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  }

  function specsList(p) {
    var sp = specs(p), keys = Object.keys(sp);
    if (!keys.length) return p.description ? '<span class="bp-specs">' + esc(p.description) + '</span>' : '';
    return '<span class="bp-specs">' + keys.map(function (k) { return '<span><i>' + esc(tSpec(k)) + ':</i> ' + esc(sp[k]) + '</span>'; }).join('') + '</span>';
  }

  function optionRow(s, p, rest) {
    var on = String(build[s.k]) === String(p.id);
    var why = conflict(p, rest);
    var status = why ? '<span class="bp-why">' + esc(why) + '</span>' : stockBadge(p);
    return '<button type="button" class="bp-opt' + (on ? ' on' : '') + '" data-pick="' + esc(p.id) + '" aria-pressed="' + on + '"' + (why ? ' disabled' : '') + '>'
      + '<span class="bp-radio" aria-hidden="true"></span>'
      + '<span class="bp-opt-main"><b>' + esc(p.name) + '</b>' + specsList(p) + status + '</span>'
      + '<span class="bp-opt-price num">' + money(p.price) + '</span></button>';
  }

  function renderStep(s) {
    var i = SLOTS.indexOf(s), rest = others(s.k);
    $('#bp-eyebrow').textContent = t('b.stepOf', { i: i + 1, n: SLOTS.length }) + (s.opt ? ' · ' + t('b.optLower') : '');
    $('#bp-title').textContent = t('b.' + s.k + '.title');
    $('#bp-hint').textContent = s.k === 'psu' ? s.hint + ' ' + t('b.psuNeed', { n: requiredPower(rest) }) : s.hint;
    document.title = (i + 1) + '. ' + s.label + ' — ' + t('nav.build') + ' — E-Pay';

    var list = products.filter(function (p) { return p._slot === s.k; })
      .sort(function (a, b) { return a.price - b.price; });
    list.sort(function (a, b) { return !conflict(b, rest) - !conflict(a, rest); });
    $('#bp-main').innerHTML = list.length
      ? '<div class="bp-opts">' + list.map(function (p) { return optionRow(s, p, rest); }).join('') + '</div>'
      : '<div class="cv-empty"><p>' + t('b.empty') + '</p></div>';
  }

  function summaryRows() {
    return SLOTS.map(function (s, i) {
      var p = picked(s.k);
      return '<li class="bp-li' + (i === step ? ' cur' : '') + (s.k === justPicked ? ' just' : '') + '"><span class="bp-li-t"><small>' + (i + 1) + ' · ' + esc(s.label) + '</small>'
        + '<span>' + (p ? esc(p.name) : '—') + '</span></span><span class="bp-li-p num">' + (p ? money(p.price) : '') + '</span></li>';
    }).join('');
  }
  function baseTotal() { return pickedParts().reduce(function (a, p) { return a + Number(p.price); }, 0); }
  function total() { return pickedParts().reduce(function (a, p) { return a + warrantyPrice(p.price, form.warranty); }, 0); }

  function renderSide() {
    var side = $('#bp-side');
    side.hidden = step >= ORDER;
    if (side.hidden) return;
    side.innerHTML = '<p class="f-title">' + t('b.yourBuild') + '</p><ul class="bp-list">' + summaryRows() + '</ul>'
      + '<div class="sum"><span>' + t('total') + '</span><b data-total>' + money(shownTotal) + '</b></div>'
      + (STORE.usdRate ? '<p class="dr-note num">' + esc(t('pd.uzs', { x: sum(uzsOf(total(), STORE.usdRate)) })) + '</p>' : '')
      + '<p class="dr-note">' + t('b.recPsu') + ' <b>' + requiredPower(pickedParts()) + 'W</b></p>';
  }

  function regionOptions() {
    return '<option value="">' + t('co.regionPh') + '</option>' + REGIONS.map(function (r) {
      return '<option value="' + esc(r.code) + '"' + (r.code === form.region ? ' selected' : '') + '>' + esc(regionName(r)) + '</option>';
    }).join('');
  }

  function renderOrder() {
    $('#bp-eyebrow').textContent = t('b.lastStep');
    document.title = t('order') + ' — ' + t('nav.build') + ' — E-Pay';
    if (placed) {
      $('#bp-title').textContent = t('order.accepted');
      $('#bp-hint').textContent = '';
      $('#bp-main').innerHTML = '<div class="bp-card done"><span class="ok-ring"><svg class="ic"><use href="#i-check"/></svg></span>'
        + '<h3>' + t('order.thanks') + '</h3><span class="order-no">' + esc(placed.number) + '</span>'
        + '<dl class="done-sum">'
        + '<div><dt>' + t('order.totalUsd') + '</dt><dd class="num">' + money(placed.totalUsd) + '</dd></div>'
        + '<div><dt>' + t('order.amountUzs') + '</dt><dd class="num">' + esc(t('uzs', { x: sum(placed.amountUzs) })) + '</dd></div>'
        + '</dl>'
        + '<p>' + t('order.callSoon') + ' ' + t('order.payOnDelivery') + '</p>'
        + '<a class="link-more" href="/#/order/' + encodeURIComponent(placed.number) + '">' + t('order.trackHint') + ' ›</a>'
        + '<a class="btn btn-ink press" href="/">' + t('b.backToShop') + '</a></div>';
      return;
    }
    var methods = STORE.paymentMethods;
    if (methods.indexOf(form.pay) < 0) form.pay = methods[0];
    $('#bp-title').textContent = t('b.placeTitle');
    $('#bp-hint').textContent = t('b.checkHint');
    $('#bp-main').innerHTML = '<div class="bp-order">'
      + '<div class="bp-card"><p class="f-title">' + t('b.build') + '</p><ul class="bp-list">' + summaryRows().replace(/ cur"/g, '"') + '</ul>'
      + '<div class="sum sum-sm"><span>' + t('b.parts') + '</span><span class="num">' + money(baseTotal()) + '</span></div>'
      + '<div class="sum sum-sm"><span>' + t('warranty.line', { y: tYears(warrantyYears(form.warranty)) })
      + (WARRANTY.filter(function (w) { return w.years === warrantyYears(form.warranty) && w.percent; }).map(function (w) { return ' (+' + w.percent + '%)'; })[0] || '')
      + '</span><span class="num">'
      + (total() > baseTotal() ? '+' + money(total() - baseTotal()) : t('inPrice')) + '</span></div>'
      + '<div class="sum"><span>' + t('b.totalParts', { x: t('parts', { n: pickedParts().length }) }) + '</span><b>' + money(total()) + '</b></div></div>'
      + '<form class="bp-card" id="bp-form" novalidate><p class="f-title">' + t('b.details') + '</p>'
      + '<label class="field">' + t('form.name') + '<input class="input" id="co-name" autocomplete="name" maxlength="100" placeholder="' + esc(t('form.namePh')) + '" value="' + esc(form.customerName) + '"><span class="err" id="name-err" hidden>' + t('form.nameErr') + '</span></label>'
      + '<label class="field">' + t('form.phone') + '<span class="phone"><span>+998</span><input id="ph" type="tel" inputmode="numeric" autocomplete="tel-national" placeholder="90 123 45 67" maxlength="12" value="' + esc(form.phone) + '"></span><span class="err" id="ph-err" hidden>' + t('form.phoneErr') + '</span></label>'
      + '<label class="field">' + t('co.region') + '<select class="input" id="co-region" required>' + regionOptions() + '</select><span class="err region-err" id="region-err" hidden>' + t('form.regionErr') + '</span></label>'
      + '<label class="field">' + t('form.addr') + '<input class="input" id="co-addr" autocomplete="street-address" maxlength="300" placeholder="' + esc(t('form.addrPh')) + '" value="' + esc(form.address) + '"><span class="err" id="addr-err" hidden>' + t('form.addrErr') + '</span></label>'
      + (WARRANTY.length > 1 ? '<div class="field">' + t('warranty.term') + '<div class="pays three">' + WARRANTY.map(function (w) {
          var extra = pickedParts().reduce(function (a, p) { return a + warrantyPrice(p.price, w.years) - Number(p.price); }, 0);
          return '<label class="pay"><input type="radio" name="warranty" value="' + w.years + '"' + (w.years === warrantyYears(form.warranty) ? ' checked' : '') + '>'
            + '<span>' + esc(tYears(w.years)) + '<small>' + (w.percent ? '+' + w.percent + '% · +' + money(extra) : t('inPrice')) + '</small></span></label>';
        }).join('') + '</div></div>' : '')
      + '<div class="field">' + t('pay.method') + '<div class="pays">' + methods.map(function (m) {
          return '<label class="pay"><input type="radio" name="pay" value="' + esc(m) + '"' + (form.pay === m ? ' checked' : '') + '><span>' + esc(payName(m)) + '</span></label>';
        }).join('') + '</div></div>'
      + '<p class="err" id="co-err" role="alert" hidden></p>'
      + '<button class="btn btn-acc btn-wide press mt-18" type="submit" id="co-submit">' + payLabel() + '</button>'
      + '</form></div>';

    var ph = $('#ph');
    ph.addEventListener('input', function () { ph.value = phoneMask(ph.value); form.phone = ph.value; $('#ph-err').hidden = true; });
    $('#co-name').addEventListener('input', function (e) { form.customerName = e.target.value; $('#name-err').hidden = true; });
    $('#co-addr').addEventListener('input', function (e) { form.address = e.target.value; $('#addr-err').hidden = true; });
    $('#co-region').addEventListener('change', function (e) { form.region = e.target.value; $('#region-err').hidden = true; });
    document.querySelectorAll('input[name="warranty"]').forEach(function (r) {
      r.addEventListener('change', function () { form.warranty = Number(r.value); render(); });
    });
    document.querySelectorAll('input[name="pay"]').forEach(function (r) {
      r.addEventListener('change', function () { form.pay = r.value; $('#co-submit').textContent = payLabel(); });
    });
    $('#bp-form').addEventListener('submit', function (e) { e.preventDefault(); submitOrder($('#co-submit')); });
  }
  function payLabel() { return t(form.pay === 'CASH' ? 'order.place' : 'order.pay', { x: money(total()) }); }

  async function submitOrder(btn) {
    var bad = false, digits = phoneDigits(form.phone);
    var name = form.customerName.trim(), address = form.address.trim();
    if (name.length < 2) { $('#name-err').hidden = false; bad = bad || $('#co-name'); }
    if (digits.length !== 9) { $('#ph-err').hidden = false; bad = bad || $('#ph'); }
    if (!form.region) { $('#region-err').hidden = false; bad = bad || $('#co-region'); }
    if (address.length < 5) { $('#addr-err').hidden = false; bad = bad || $('#co-addr'); }
    if (bad) { bad.focus(); return; }

    btn.disabled = true; btn.textContent = t('order.sending');
    $('#co-err').hidden = true;
    try {
      var problem = await serverCompatCheck(pickedParts());
      if (problem) throw new Error(problem);
      var order = await api('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          customerName: name, phone: '+998' + digits, address: address, region: form.region, paymentMethod: form.pay,
          items: pickedParts().map(function (p) { return { productId: p.id, quantity: 1, warrantyYears: warrantyYears(form.warranty) }; })
        })
      });
      store('lastOrder', { number: order.number, phone: digits });
      build = {}; saveBuild();
      placed = order;
      render();
      window.scrollTo(0, 0);
    } catch (err) {
      btn.disabled = false; btn.textContent = payLabel();
      $('#co-err').textContent = err.status === 429 ? t('err.429', { n: err.retryAfter || 60 }) : err.message;
      $('#co-err').hidden = false;
    }
  }

  function renderNav() {
    var nav = $('#bp-nav');
    if (placed) { nav.innerHTML = ''; return; }
    var prev = step === 0
      ? '<a class="btn btn-ghost press" href="/"><svg class="ic bp-back"><use href="#i-arrow"/></svg>' + t('b.shop') + '</a>'
      : '<a class="btn btn-ghost press" href="' + hrefFor(step - 1) + '"><svg class="ic bp-back"><use href="#i-arrow"/></svg>' + t('b.back') + '</a>';
    var next = '';
    if (step < ORDER) {
      var s = SLOTS[step], nextLabel = step + 1 < SLOTS.length ? SLOTS[step + 1].label : t('order');
      if (picked(s.k)) next = '<a class="btn btn-acc press" href="' + hrefFor(step + 1) + '">' + esc(t('b.next', { x: nextLabel })) + '<svg class="ic"><use href="#i-arrow"/></svg></a>';
      else if (s.opt) next = '<a class="btn btn-ink press" href="' + hrefFor(step + 1) + '">' + t('b.skip') + '<svg class="ic"><use href="#i-arrow"/></svg></a>';
      else next = '<button class="btn btn-acc" type="button" disabled>' + t('b.' + s.k + '.first') + '</button>';
    }
    nav.innerHTML = '<div class="wrap bp-nav-in">' + prev
      + '<span class="bp-nav-sum"><small>' + t('total') + '</small><b class="num" data-total>' + money(shownTotal) + '</b></span>' + next + '</div>';
  }

  document.addEventListener('click', function (e) {
    var pk = e.target.closest('[data-pick]');
    if (!pk || step >= ORDER) return;
    var s = SLOTS[step], id = pk.getAttribute('data-pick');
    var again = String(build[s.k]) === id;
    pick(s.k, again ? null : id);
    if (!again) justPicked = s.k;
    render();
  });
  window.addEventListener('hashchange', function () {
    var prev = step;
    step = parseRoute();
    if (step !== prev) enterDir = step > prev ? 'r' : 'l';
    render();
    window.scrollTo(0, 0);
  });

  var toastTimer = 0;
  function showToast(msg) {
    $('#toast-text').textContent = msg;
    $('#toast').classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { $('#toast').classList.remove('show'); }, 3000);
  }

  async function loadParts() {
    var categories = await api('/api/categories');
    var lists = await Promise.all(SLOTS.map(function (s) {
      var c = categories.filter(function (x) { return s.match.test(x.name); })[0];
      if (!c) return Promise.resolve([]);
      return api('/api/products?categoryId=' + encodeURIComponent(c.id) + '&size=100').then(function (page) {
        return Promise.all(page.items.map(function (card) {
          return api('/api/products/' + encodeURIComponent(card.id)).then(function (p) { p._slot = s.k; return p; });
        }));
      });
    }));
    return [].concat.apply([], lists);
  }

  loadStorefront().then(function () { if (ready) render(); }).catch(function () {});
  loadRegions().then(function () { if (ready && step >= ORDER && !placed) render(); }).catch(function () {});

  loadParts().then(function (list) {
    products = list;
    ready = true;
    step = parseRoute();
    render();
  }).catch(function (err) {
    $('#bp-main').innerHTML = '<div class="cv-empty"><p>' + esc(err.message || t('loadErr')) + '</p></div>';
  });
})();
