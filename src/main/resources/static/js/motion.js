var REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function tweenNumber(el, from, to, fmt, ms) {
  if (!el) return;
  fmt = fmt || function (n) { return String(Math.round(n)); };
  if (REDUCED_MOTION || from === to || !isFinite(from)) { el.textContent = fmt(to); return; }
  var start = performance.now(), dur = ms || 450;
  if (el._tween) cancelAnimationFrame(el._tween);
  clearTimeout(el._tweenEnd);
  el._tweenEnd = setTimeout(function () { cancelAnimationFrame(el._tween); el.textContent = fmt(to); }, dur + 150);
  (function frame(now) {
    var t = Math.min(1, (now - start) / dur), k = 1 - Math.pow(1 - t, 3);
    el.textContent = fmt(t < 1 ? from + (to - from) * k : to);
    if (t < 1) el._tween = requestAnimationFrame(frame);
  })(start);
}

function flyTo(fromEl, toEl, done) {
  if (REDUCED_MOTION || !fromEl || !toEl || !document.body.animate) { if (done) done(); return; }
  var card = fromEl.closest('.card, .prow') || fromEl.closest('.pd-wrap') || fromEl.closest('main') || document.body;
  var imgEl = card.querySelector('img.pimg') || card.querySelector('img');
  var a = fromEl.getBoundingClientRect();
  var b = toEl.getBoundingClientRect();

  var c = document.createElement('div');
  c.className = 'courier';
  c.setAttribute('aria-hidden', 'true');
  c.innerHTML = '<div class="courier-box"></div>'
    + '<div class="courier-flip"><svg class="courier-man" viewBox="0 0 24 36">'
    + '<circle class="cm-head" cx="12" cy="7" r="4"/>'
    + '<path d="M12 11v12"/>'
    + '<path class="cm-arms-up" d="M12 14L6 5M12 14l6-9"/>'
    + '<path class="cm-arms-down cm-arm-l" d="M12 14l-4 7"/><path class="cm-arms-down cm-arm-r" d="M12 14l4 7"/>'
    + '<path class="cm-leg cm-leg-l" d="M12 23l-3 11"/><path class="cm-leg cm-leg-r" d="M12 23l3 11"/>'
    + '</svg></div>';
  var box = c.querySelector('.courier-box'), flip = c.querySelector('.courier-flip');
  if (imgEl && imgEl.currentSrc) {
    var img = document.createElement('img');
    img.alt = '';
    img.src = imgEl.currentSrc;
    box.appendChild(img);
  }
  document.body.appendChild(c);

  var x0 = a.left + a.width / 2, y0 = a.top - 52;
  var dx = b.left + b.width / 2 - x0, dy = b.top + b.height / 2 - y0;
  c.style.left = (x0 - 18) + 'px';
  c.style.top = (y0 - 15) + 'px';
  var face = dx < 0 ? -1 : 1;
  flip.style.transform = 'scaleX(' + face + ')';
  var walk = Math.max(550, Math.min(1100, Math.hypot(dx, dy) * 0.9));
  var there = 'translate(' + dx + 'px, ' + dy + 'px)';

  c.animate([{ transform: 'translateY(12px) scale(0.3)', opacity: 0 }, { transform: 'none', opacity: 1 }],
    { duration: 220, easing: 'cubic-bezier(0.34, 1.56, 0.64, 1)' }).onfinish = function () {
    c.classList.add('is-walking');
    c.animate([{ transform: 'none' }, { transform: there }],
      { duration: walk, easing: 'cubic-bezier(0.45, 0, 0.4, 1)', fill: 'forwards' }).onfinish = function () {
      c.classList.remove('is-walking');
      box.animate([
        { transform: 'none', opacity: 1 },
        { transform: 'translateY(-6px) scale(1.1)', opacity: 1, offset: 0.35 },
        { transform: 'scale(0.15)', opacity: 0 }
      ], { duration: 260, easing: 'ease-in', fill: 'forwards' }).onfinish = function () {
        if (toEl.animate) {
          toEl.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.4)' }, { transform: 'scale(1)' }],
            { duration: 350, easing: 'cubic-bezier(0.34, 1.56, 0.64, 1)' });
        }
        if (done) done();
        c.classList.add('is-empty', 'is-walking');
        flip.style.transform = 'scaleX(' + (-face) + ')';
        c.animate([
          { transform: there, opacity: 1 },
          { transform: 'translate(' + dx * 0.25 + 'px, ' + dy * 0.25 + 'px)', opacity: 1, offset: 0.8 },
          { transform: 'none', opacity: 0 }
        ], { duration: walk * 0.8, easing: 'ease-in-out', fill: 'forwards' }).onfinish = function () { c.remove(); };
      };
    };
  };
}

(function () {
  if (REDUCED_MOTION || !document.body.hasAttribute('data-mouse')
      || !window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
  var TILT = '.card, .cat, .promo-car', MAGNET = '.btn.press';
  var tilt = null, magnet = null, ev = null, raf = 0;

  function clear(el, cls, props) {
    if (!el) return;
    el.classList.remove(cls);
    props.forEach(function (p) { el.style.removeProperty(p); });
  }
  function clamp(v, max) { return Math.max(-max, Math.min(max, v)); }

  function frame() {
    raf = 0;
    var target = ev && ev.target && ev.target.closest ? ev.target : null;

    var t = target && target.closest(TILT);
    if (t !== tilt) { clear(tilt, 'is-tilt', ['--rx', '--ry', '--mx', '--my', '--px', '--py']); tilt = t; }
    if (t) {
      var r = t.getBoundingClientRect();
      var x = (ev.clientX - r.left) / r.width, y = (ev.clientY - r.top) / r.height;
      var shift = t.classList.contains('promo-car') ? -16 : 8;
      t.style.setProperty('--mx', (x * 100).toFixed(1) + '%');
      t.style.setProperty('--my', (y * 100).toFixed(1) + '%');
      t.style.setProperty('--px', ((x - 0.5) * shift).toFixed(1) + 'px');
      t.style.setProperty('--py', ((y - 0.5) * shift).toFixed(1) + 'px');
      if (t.classList.contains('card') || t.classList.contains('cat')) {
        var isCat = t.classList.contains('cat');
        t.style.setProperty('--rx', ((0.5 - y) * (isCat ? 8 : 7.5)).toFixed(2) + 'deg');
        t.style.setProperty('--ry', ((x - 0.5) * (isCat ? 10 : 9.5)).toFixed(2) + 'deg');
      }
      t.classList.add('is-tilt');
    }

    var m = target && target.closest(MAGNET);
    if (m && m.disabled) m = null;
    if (m !== magnet) { clear(magnet, 'is-magnet', ['--bx', '--by']); magnet = m; }
    if (m) {
      var b = m.getBoundingClientRect();
      m.style.setProperty('--bx', clamp((ev.clientX - b.left - b.width / 2) * 0.3, 7).toFixed(1) + 'px');
      m.style.setProperty('--by', clamp((ev.clientY - b.top - b.height / 2) * 0.4, 5).toFixed(1) + 'px');
      m.classList.add('is-magnet');
    }
  }

  document.addEventListener('pointermove', function (e) {
    if (e.pointerType !== 'mouse') return;
    ev = e;
    if (!raf) raf = requestAnimationFrame(frame);
  }, { passive: true });
  document.addEventListener('mouseout', function (e) {
    if (e.relatedTarget) return;
    ev = null;
    if (!raf) raf = requestAnimationFrame(frame);
  });
})();

function revealOnScroll(root, selector) {
  if (!root || REDUCED_MOTION || !('IntersectionObserver' in window)) return;
  if (root._reveal) root._reveal.disconnect();
  var io = root._reveal = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (!e.isIntersecting) return;
      e.target.classList.add('is-in');
      io.unobserve(e.target);
    });
  }, { threshold: 0.15, rootMargin: '0px 0px -6% 0px' });
  root.querySelectorAll(selector).forEach(function (el) {
    el.classList.remove('is-in');
    el.classList.add('reveal');
    io.observe(el);
  });
}

function staggerOnce(el, ms) {
  if (!el || REDUCED_MOTION) return;
  el.classList.add('stagger');
  clearTimeout(el._stagger);
  el._stagger = setTimeout(function () { el.classList.remove('stagger'); }, ms || 900);
}
