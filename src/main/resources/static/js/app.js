(function () {
  var reduced = REDUCED_MOTION;
  var $ = function (s) { return document.querySelector(s); };
  var PAGE_SIZE = 20;
  var HOME_SIZE = 100;
  var DEBOUNCE_MS = 300;

  var categories = [], brands = [], home = [], homeTotal = 0, homeLoaded = false;
  var cart = load('cart', {});

  function saveCart() { store('cart', cart); }
  function unitPrice(price, warranty) { return warrantyPrice(price, warranty); }
  function prodHref(p) { return '#/p/' + encodeURIComponent(p.id); }
  function countsKnown() { return homeLoaded && home.length >= homeTotal; }

  function addBtnHTML(p, extraCls) {
    var inCart = !!cart[p.id];
    return '<button class="btn btn-acc add press' + (extraCls || '') + '" type="button" data-act="cart" data-id="' + esc(p.id) + '" aria-pressed="' + inCart + '">'
      + '<svg class="ic"><use href="' + (inCart ? '#i-check' : '#i-bag') + '"/></svg><span>' + t(inCart ? 'cart.in' : 'cart.add') + '</span></button>';
  }

  var NEW_IDS = {};
  function markNew() {
    NEW_IDS = {};
    home.slice(0, 10).forEach(function (p) { NEW_IDS[p.id] = true; });
  }
  function fromPrice(p) { return t('price.from', { x: priceHTML(p.price) }); }
  function metaLine(p) { return [brandNameOf(p), tCat(catNameOf(p))].filter(Boolean).join(' · '); }

  function artLink(p, cls) {
    return '<a class="' + cls + (mainImage(p) ? ' has-img' : '') + '" href="' + prodHref(p) + '" tabindex="-1" aria-hidden="true">' + photo(p, 'ill-lg') + '</a>';
  }
  function card(p) {
    return '<article class="card" id="p-' + esc(p.id) + '">'
      + '<div class="body">'
      + (NEW_IDS[p.id] ? '<p class="new-tag">' + t('new.tag') + '</p>' : '')
      + '<h3 class="name"><a href="' + prodHref(p) + '">' + esc(p.name) + '</a></h3>'
      + artLink(p, 'card-art')
      + '<p class="specs">' + esc(metaLine(p)) + '</p>'
      + '<div class="foot">'
      + '<p class="price">' + fromPrice(p) + '</p>'
      + addBtnHTML(p)
      + '</div></div></article>';
  }
  function prow(p) {
    return '<article class="prow" id="p-' + esc(p.id) + '">'
      + artLink(p, 'prow-img')
      + '<div class="prow-mid">'
      + (NEW_IDS[p.id] ? '<p class="new-tag">' + t('new.tag') + '</p>' : '')
      + '<h3 class="prow-name"><a href="' + prodHref(p) + '">' + esc(p.name) + '</a></h3>'
      + '<p class="prow-code">' + t('row.sku') + ' <span>' + esc(p.sku) + '</span>' + (brandNameOf(p) ? ' · ' + esc(brandNameOf(p)) : '') + '</p>'
      + '<p class="prow-desc">' + esc(tCat(catNameOf(p))) + '</p>'
      + '</div>'
      + '<div class="prow-buy"><p class="prow-price">' + priceHTML(p.price) + '</p>' + stockBadge(p)
      + addBtnHTML(p, ' prow-add')
      + '<a class="btn btn-ghost prow-more" href="' + prodHref(p) + '">' + t('more') + '</a></div>'
      + '</article>';
  }

  var known = {};
  function remember(list) { list.forEach(function (p) { known[p.id] = p; }); }
  function byId(id) { return known[id]; }

  function brandBySlug(s) { return brands.filter(function (b) { return b.slug === s; })[0]; }
  function brandCount(b) { return home.filter(function (p) { return p.brandId === b.id; }).length; }
  function catCount(id) { return home.filter(function (p) { return p.categoryId === id; }).length; }
  function brandLogo(b) {
    var src = safeImg(b.logo);
    return src ? '<img src="' + esc(src) + '" alt="' + esc(b.name) + '" data-brand-logo loading="lazy" decoding="async">'
      : '<span class="partner-ph">' + esc(b.name) + '</span>';
  }
  function brandTile(b) {
    return '<li><a class="brand-tile press" href="#/b/' + esc(b.slug) + '">'
      + '<span class="brand-logo">' + brandLogo(b) + '</span>'
      + '<b>' + esc(b.name) + '</b>'
      + (countsKnown() ? '<small>' + t('items', { n: brandCount(b) }) + '</small>' : '') + '</a></li>';
  }
  document.addEventListener('error', function (e) {
    var img = e.target;
    if (!img || img.tagName !== 'IMG' || !img.hasAttribute('data-brand-logo')) return;
    var s = document.createElement('span');
    s.className = 'partner-ph';
    s.textContent = img.alt;
    img.replaceWith(s);
  }, true);

  function renderCatMenu() {
    function cur(id) { return route.name === 'cat' && !route.brand && String(id) === route.cat ? ' aria-current="page"' : ''; }
    $('#cat-menu').innerHTML = '<a class="cm-all" role="menuitem" href="#/c/all"' + cur('all') + '>'
      + '<span class="cm-ic"><svg class="ic"><use href="#i-grid"/></svg></span>'
      + '<span class="cm-t"><b>' + t('allProducts') + '</b>' + (homeLoaded ? '<small>' + t('items', { n: homeTotal }) + '</small>' : '') + '</span>'
      + '<svg class="ic cm-go"><use href="#i-arrow"/></svg></a>'
      + '<div class="cm-grid">' + categories.map(function (c) {
          return '<a class="cm-tile" role="menuitem" href="#/c/' + esc(c.id) + '"' + cur(c.id) + '>'
            + '<span class="cm-ic"><svg class="ill ill-sm" viewBox="0 0 64 64" aria-hidden="true"><use href="#' + illFor(c.name) + '"/></svg></span>'
            + '<span class="cm-t"><b>' + esc(tCat(c.name)) + '</b>' + (countsKnown() ? '<small>' + t('items', { n: catCount(c.id) }) + '</small>' : '') + '</span></a>';
        }).join('') + '</div>';
  }
  function setCatMenu(open, byHover) {
    var menu = $('#cat-menu'), btn = $('#cat-btn');
    if (open && !categories.length) { if (!byHover) location.hash = '#/c/all'; return; }
    if (open && menu.hidden) renderCatMenu();
    menu.hidden = !open;
    btn.setAttribute('aria-expanded', String(open));
    $('#cat-dd').classList.toggle('open', open);
    hoverOpen = open && !!byHover;
    if (open && !byHover) (menu.querySelector('[aria-current]') || menu.firstChild).focus();
  }
  var hoverOpen = false, catTimer = 0, catPicked = false;
  if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
    $('#cat-dd').addEventListener('mouseenter', function () {
      clearTimeout(catTimer);
      if (catPicked) return;
      catTimer = setTimeout(function () { if ($('#cat-menu').hidden) setCatMenu(true, true); }, 120);
    });
    $('#cat-dd').addEventListener('mouseleave', function () {
      clearTimeout(catTimer);
      catPicked = false;
      catTimer = setTimeout(function () { if (hoverOpen) setCatMenu(false); }, 280);
    });
  }
  $('#cat-btn').addEventListener('click', function () {
    if (hoverOpen) { hoverOpen = false; return; }
    setCatMenu($('#cat-menu').hidden);
  });
  $('#cat-menu').addEventListener('click', function (e) {
    var a = e.target.closest('a'); if (!a) return;
    clearTimeout(catTimer);
    catPicked = true;
    setCatMenu(false);
    a.blur();
  });
  $('#cat-menu').addEventListener('keydown', function (e) {
    var items = [].slice.call(this.querySelectorAll('a')), i = items.indexOf(document.activeElement);
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') { e.preventDefault(); items[(i + (e.key === 'ArrowDown' ? 1 : items.length - 1)) % items.length].focus(); }
  });
  document.addEventListener('click', function (e) { if (!e.target.closest('#cat-dd') && !$('#cat-menu').hidden) setCatMenu(false); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !$('#cat-menu').hidden) { setCatMenu(false); $('#cat-btn').focus(); } });

  function linkCategories() {
    document.querySelectorAll('[data-cat-link]').forEach(function (a) {
      var re = new RegExp(a.getAttribute('data-cat-link'), 'i');
      var c = categories.filter(function (x) { return re.test(x.name); })[0];
      if (c) a.setAttribute('href', '#/c/' + c.id);
    });
  }

  function renderPromos() {
    var list = (typeof PROMOS === 'undefined' ? [] : PROMOS).map(function (b) {
      return { b: b, p: home.filter(function (x) { return x.sku === b.sku; })[0] };
    }).filter(function (x) { return x.p && safeImg(x.b.img); });
    promoIndex = Math.min(promoIndex, Math.max(list.length - 1, 0));
    $('#promo-sec').hidden = !list.length || route.name !== 'home';
    $('#promo-dots').innerHTML = list.length > 1 ? list.map(function (x, j) {
      return '<button class="dot" type="button" aria-label="' + esc(x.b.title) + '" aria-current="' + (j === promoIndex) + '"></button>';
    }).join('') : '';
    $('#promos').innerHTML = list.map(function (x, j) {
      var p = x.p;
      return '<article class="promo' + (j === promoIndex ? ' is-active' : '') + '" aria-roledescription="slide" aria-label="' + (j + 1) + ' / ' + list.length + '">'
        + '<div class="promo-txt">'
        + (NEW_IDS[p.id] ? '<p class="new-tag">' + t('new.tag') + '</p>' : '')
        + '<h3>' + esc(x.b.title) + '</h3>'
        + '<p class="promo-lede">' + esc(t(x.b.text)) + '</p>'
        + '<p class="price">' + fromPrice(p) + '</p>'
        + '<div class="tile-links">' + addBtnHTML(p)
        + '<a class="link-more" href="' + prodHref(p) + '">' + t('more') + ' ›</a></div>'
        + '</div>'
        + '<img class="promo-img" src="' + esc(safeImg(x.b.img)) + '" alt="' + esc(x.b.title) + '" decoding="async">'
        + '</article>';
    }).join('');
    promoRestart();
  }

  var PROMO_MS = 5000, promoIndex = 0, promoTimer = 0, promoHold = false, promoX = 0;
  function promoGo(i) {
    var slides = $('#promos').children; if (!slides.length) return;
    var next = (i + slides.length) % slides.length;
    if (next === promoIndex) return;
    [].forEach.call(slides, function (s, j) {
      s.classList.toggle('is-leaving', j === promoIndex);
      s.classList.toggle('is-active', j === next);
    });
    promoIndex = next;
    [].forEach.call($('#promo-dots').children, function (d, j) { d.setAttribute('aria-current', String(j === promoIndex)); });
  }
  function promoRestart() {
    clearInterval(promoTimer);
    var stop = promoHold || document.hidden || route.name !== 'home' || $('#promos').children.length < 2;
    $('#promo-car').classList.toggle('paused', stop);
    if (stop) return;
    promoTimer = setInterval(function () { promoGo(promoIndex + 1); }, PROMO_MS);
  }
  (function () {
    var box = $('#promo-car');
    box.style.setProperty('--promo-ms', PROMO_MS + 'ms');
    function hold(on) { promoHold = on; promoRestart(); }
    box.addEventListener('mouseenter', function () { hold(true); });
    box.addEventListener('mouseleave', function () { hold(false); });
    box.addEventListener('focusin', function () { hold(true); });
    box.addEventListener('focusout', function (e) { if (!box.contains(e.relatedTarget)) hold(false); });
    box.addEventListener('touchstart', function (e) { promoX = e.touches[0].clientX; hold(true); }, { passive: true });
    box.addEventListener('touchend', function (e) {
      var dx = e.changedTouches[0].clientX - promoX;
      if (Math.abs(dx) > 40) promoGo(promoIndex + (dx < 0 ? 1 : -1));
      hold(false);
    }, { passive: true });
    $('#promo-dots').addEventListener('click', function (e) {
      var d = e.target.closest('.dot'); if (!d) return;
      promoGo([].indexOf.call(d.parentNode.children, d)); promoRestart();
    });
    box.addEventListener('mouseleave', function () {
      var d = $('#promo-dots [aria-current="true"]'); if (!d) return;
      d.setAttribute('aria-current', 'false'); void d.offsetWidth; d.setAttribute('aria-current', 'true');
    });
    document.addEventListener('visibilitychange', promoRestart);
  })();

  function renderHome() {
    markNew();
    linkCategories();
    renderPromos();
    $('#cats').innerHTML = categories.map(function (c) {
      var pic = home.filter(function (p) { return p.categoryId === c.id && safeImg(p.imageUrl); })[0];
      var src = pic ? safeImg(pic.imageUrl) : '';
      return '<li><a class="hcat" href="#/c/' + esc(c.id) + '">'
        + '<span class="hcat-media' + (src ? ' has-img' : '') + '" aria-hidden="true">'
        + (src ? '<img class="pimg" src="' + esc(src) + '" alt="" loading="lazy" decoding="async">' : '')
        + '<svg class="ill ill-sm" viewBox="0 0 64 64"><use href="#' + illFor(c.name) + '"/></svg></span>'
        + '<span class="hcat-body"><span class="hcat-name">' + esc(tCat(c.name)) + '</span>'
        + '<span class="hcat-foot">' + (countsKnown() ? '<span class="hcat-count">' + t('items', { n: catCount(c.id) }) + '</span>' : '<span></span>')
        + '<span class="hcat-go">' + t('cat.go') + '<svg class="ic"><use href="#i-arrow"/></svg></span></span></span></a></li>';
    }).join('');
    $('#brands').innerHTML = brands.map(brandTile).join('');
    $('#brands-sec').hidden = !brands.length || route.name !== 'brands';
    var featured = home.slice().sort(function (a, b) { return !!mainImage(b) - !!mainImage(a); });
    $('#featured').innerHTML = featured.slice(0, 8).map(card).join('');
    $('#new').innerHTML = home.slice(0, 10).map(card).join('');
    renderCatMenu();
    ['#cats', '#brands', '#featured', '#new'].forEach(function (s) { staggerOnce($(s)); });
  }

  function updateCounts(bump) {
    var n = cartQty();
    document.querySelectorAll('[data-count="cart"]').forEach(function (b) {
      b.textContent = n;
      b.setAttribute('data-n', n);
      if (bump && !reduced && b.animate) {
        b.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.5)' }, { transform: 'scale(1)' }],
          { duration: 380, easing: 'cubic-bezier(.34,1.56,.64,1)' });
      }
    });
  }

  function cartIds() { return Object.keys(cart).filter(function (k) { return cart[k] && cart[k].qty > 0; }); }
  function cartQty() { return cartIds().reduce(function (s, k) { return s + cart[k].qty; }, 0); }
  function cartTotal() {
    return cartIds().reduce(function (s, k) { var r = cart[k]; return s + unitPrice(r.price, r.warranty) * r.qty; }, 0);
  }
  function syncCartButtons() {
    document.querySelectorAll('[data-act="cart"]').forEach(function (x) {
      var on = !!cart[x.getAttribute('data-id')];
      x.setAttribute('aria-pressed', String(on));
      x.querySelector('use').setAttribute('href', on ? '#i-check' : '#i-bag');
      x.querySelector('span').textContent = t(on ? 'cart.in' : (x.getAttribute('data-label') || 'cart.add'));
    });
  }

  document.addEventListener('click', function (e) {
    var b = e.target.closest('[data-act="cart"]');
    if (!b) return;
    var id = b.getAttribute('data-id');
    if (cart[id]) { openCart(); return; }
    var p = byId(id);
    if (!p) return;
    if (!cartAdd(cart, p)) { showToast(t('cart.max', { n: MAX_QTY }), false); return; }
    if (b.hasAttribute('data-w')) cart[id].warranty = Number(b.getAttribute('data-w'));
    saveCart(); syncCartButtons(); updateCounts(false);
    if (b.animate) {
      b.animate([
        { transform: 'scale(1)' },
        { transform: 'scale(0.92)' },
        { transform: 'scale(1.06)' },
        { transform: 'scale(1)' }
      ], { duration: 320, easing: 'cubic-bezier(0.34, 1.56, 0.64, 1)' });
    }
    var target = [].slice.call(document.querySelectorAll('.act-cart, .tabbar [data-open="cart"]'))
      .filter(function (x) { return x.offsetParent !== null; })[0];
    flyTo(b, target, function () { updateCounts(true); });
    showToast(t('cart.added'), true);
  });

  var route = { name: 'home', cat: null };

  function parseRoute() {
    var h = location.hash || '#/';
    var o = h.match(/^#\/order(?:\/([A-Za-z0-9-]+))?/);
    if (o) return { name: 'order', cat: null, id: o[1] ? decodeURIComponent(o[1]) : '' };
    if (/^#\/about(?:$|[/?])/.test(h)) return { name: 'about', cat: null };
    var pg = h.match(/^#\/(cats|brands|featured|new)(?:$|[/?])/);
    if (pg) return { name: pg[1], cat: null };
    var pm = h.match(/^#\/p\/(\d+)/);
    if (pm) return { name: 'prod', cat: null, id: pm[1] };
    var bm = h.match(/^#\/b\/([a-z0-9-]+)(?:\/c\/(\d+))?/);
    if (bm) return { name: 'cat', cat: bm[2] || 'all', brand: bm[1] };
    var m = h.match(/^#\/c\/(all|\d+)/);
    return m ? { name: 'cat', cat: m[1] } : { name: 'home', cat: null };
  }
  function homeSections() {
    return [].slice.call(document.querySelectorAll('main > section')).filter(function (x) {
      return ['catview', 'orderview', 'prodview', 'aboutview'].indexOf(x.id) < 0;
    });
  }
  function applyRoute(scroll) {
    var prevKey = route.brand + '|' + route.cat;
    route = parseRoute();
    var isCat = route.name === 'cat', isOrder = route.name === 'order', isProd = route.name === 'prod', isAbout = route.name === 'about';
    homeSections().forEach(function (x) { x.hidden = (x.dataset.view || 'home') !== route.name; });
    if (!brands.length) $('#brands-sec').hidden = true;
    if (route.name === 'home' && !$('#promos').children.length) $('#promo-sec').hidden = true;
    $('#catview').hidden = !isCat;
    $('#orderview').hidden = !isOrder;
    $('#prodview').hidden = !isProd;
    $('#aboutview').hidden = !isAbout;
    document.querySelectorAll('#hdr [data-page]').forEach(function (a) {
      if (a.dataset.page === route.name) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
    document.querySelectorAll('.tabbar [data-tab]').forEach(function (a) {
      a.setAttribute('aria-current', a.dataset.tab === route.name ? 'page' : 'false');
    });
    if (isOrder) {
      openTrack(route.id);
      document.title = t('track.title') + ' — E-Pay';
    } else if (isCat) {
      if (prevKey !== route.brand + '|' + route.cat) { cv.brand = ''; }
      renderCatHead();
      loadCatalog(true);
      document.title = viewName() + ' — E-Pay';
    } else if (isProd) {
      loadProduct(route.id);
    } else if (isAbout) {
      revealOnScroll($('#aboutview'), '.ab-hero-txt > *, .ab-feat, .ab-center, .ab-contact');
      document.title = t('nav.about') + ' — E-Pay';
    } else if (/^(cats|brands|featured|new)$/.test(route.name)) {
      document.title = t('nav.' + route.name) + ' — E-Pay';
    } else {
      document.title = 'E-Pay — ' + t('meta.title');
    }
    promoRestart();
    if (scroll !== false) window.scrollTo(0, 0);
  }

  function catName(k) {
    if (k === 'all') return t('allProducts');
    var c = categories.filter(function (x) { return String(x.id) === String(k); })[0];
    return c ? tCat(c.name) : t('nav.catalog');
  }
  function viewName() {
    if (!route.brand) return catName(route.cat);
    var b = brandBySlug(route.brand), n = b ? b.name : t('sec.brands');
    return route.cat === 'all' ? n : n + ' · ' + catName(route.cat);
  }

  var cv = { q: '', brand: '', page: 0, items: [], total: 0, totalPages: 0, ctrl: null, seq: 0 };

  function renderCatHead() {
    var brand = route.brand ? brandBySlug(route.brand) : null;
    if (brand) {
      var bHref = '#/b/' + esc(brand.slug);
      $('#cv-crumb').innerHTML = '<a href="/brands.html">' + t('nav.brands') + '</a><span>/</span>'
        + (route.cat === 'all' ? '<span>' + esc(brand.name) + '</span>'
          : '<a href="' + bHref + '">' + esc(brand.name) + '</a><span>/</span><span>' + esc(catName(route.cat)) + '</span>');
      $('#cv-title').textContent = t('brand.products', { x: brand.name });
    } else {
      $('#cv-crumb').textContent = viewName();
      $('#cv-title').textContent = viewName();
    }
    var meta = brand ? [brand.country, brand.founded ? t('brand.since', { y: brand.founded }) : ''].filter(Boolean) : [];
    var site = brand ? safeHttps(brand.website) : '';
    var about = brand && (brand.description || meta.length || site);
    $('#cv-about').hidden = !about;
    $('#cv-about').innerHTML = about ? (
      (meta.length ? '<p class="bcard-meta">' + esc(meta.join(' · ')) + '</p>' : '')
      + (brand.description ? '<p class="bcard-desc">' + esc(brand.description) + '</p>' : '')
      + '<p class="bcard-links"><a class="link-more" href="/brands.html#' + esc(brand.slug) + '">' + t('brand.more') + ' ›</a>'
      + (site ? '<a class="link-more" href="' + esc(site) + '" target="_blank" rel="noopener noreferrer">' + t('brand.site') + ' ↗</a>' : '')
      + '</p>') : '';
    $('#cv-logo').hidden = !brand;
    $('#cv-logo').innerHTML = brand ? brandLogo(brand) : '';
    var base = route.brand ? '#/b/' + esc(route.brand) : '#/c';
    var all = [{ id: 'all', name: route.brand ? t('brand.all') : t('allProducts') }].concat(categories);
    $('#cv-chips').hidden = false;
    $('#cv-cats').hidden = true;
    $('#cv-chips').innerHTML = all.map(function (c) {
      var href = route.brand ? base + (c.id === 'all' ? '' : '/c/' + c.id) : base + '/' + c.id;
      return '<a class="chip press" href="' + href + '" aria-current="' + (String(c.id) === route.cat ? 'page' : 'false') + '">'
        + esc(c.id === 'all' ? c.name : tCat(c.name)) + '</a>';
    }).join('');
    if (route.brand) {
      $('#sc-title').textContent = t('sec.brands');
      $('#cv-side').innerHTML = brands.map(function (b) {
        return '<li><a class="sc" href="#/b/' + esc(b.slug) + '" aria-current="' + (b.slug === route.brand ? 'page' : 'false') + '">'
          + '<span>' + esc(b.name) + '</span>' + (countsKnown() ? '<small>' + brandCount(b) + '</small>' : '') + '</a></li>';
      }).join('');
    } else {
      $('#sc-title').textContent = t('sec.categories');
      $('#cv-side').innerHTML = all.map(function (c) {
        var n = c.id === 'all' ? homeTotal : catCount(c.id);
        return '<li><a class="sc" href="#/c/' + c.id + '" aria-current="' + (String(c.id) === route.cat ? 'page' : 'false') + '">'
          + '<span>' + esc(c.id === 'all' ? c.name : tCat(c.name)) + '</span>' + (countsKnown() ? '<small>' + n + '</small>' : '') + '</a></li>';
      }).join('');
    }
    var bl = route.brand ? [] : brands;
    $('#cv-brands').hidden = bl.length < 2;
    $('#br-row').innerHTML = bl.length < 2 ? '' : '<button class="br-tile all" type="button" data-fx-brand="" aria-pressed="' + !cv.brand + '"><b>' + t('f.all') + '</b></button>'
      + bl.map(function (b) {
        return '<button class="br-tile" type="button" data-fx-brand="' + esc(b.id) + '" aria-pressed="' + (cv.brand === String(b.id)) + '">'
          + '<span class="br-logo">' + brandLogo(b) + '</span></button>';
      }).join('');
    if ($('#cv-q').value !== cv.q) $('#cv-q').value = cv.q;
    var chips = [];
    if (cv.q) chips.push(['q', '“' + cv.q + '”']);
    if (cv.brand) { var fb = brands.filter(function (x) { return String(x.id) === cv.brand; })[0]; if (fb) chips.push(['brand', fb.name]); }
    $('#fx-chips').hidden = !chips.length;
    $('#f-reset').hidden = !chips.length;
    $('#fx-chips').innerHTML = chips.map(function (c) {
      return '<button class="fx-chip" type="button" data-fx-del="' + esc(c[0]) + '" aria-label="' + esc(t('f.remove', { x: c[1] })) + '">' + esc(c[1])
        + '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6L6 18M6 6l12 12"/></svg></button>';
    }).join('');
  }

  function catalogParams(page) {
    var params = new URLSearchParams();
    if (cv.q) params.set('q', cv.q);
    if (route.cat && route.cat !== 'all') params.set('categoryId', route.cat);
    var b = route.brand ? brandBySlug(route.brand) : null;
    if (b) params.set('brandId', b.id);
    else if (cv.brand) params.set('brandId', cv.brand);
    params.set('page', page);
    params.set('size', PAGE_SIZE);
    return params;
  }
  async function loadCatalog(reset) {
    if (route.name !== 'cat') return;
    if (route.brand && brands.length && !brandBySlug(route.brand)) {
      cv.items = []; cv.total = 0; cv.totalPages = 0; renderCatalog(); return;
    }
    if (route.brand && !brands.length) return;
    if (cv.ctrl) cv.ctrl.abort();
    var ctrl = cv.ctrl = new AbortController(), seq = ++cv.seq;
    var page = reset ? 0 : cv.page + 1;
    $('#cv-grid').classList.add('cv-loading');
    $('#cv-more-btn').disabled = true;
    try {
      var data = await api('/api/products?' + catalogParams(page), { signal: ctrl.signal });
      if (seq !== cv.seq) return;
      remember(data.items);
      cv.items = reset ? data.items : cv.items.concat(data.items);
      cv.page = data.page; cv.total = data.totalElements; cv.totalPages = data.totalPages;
      renderCatalog(reset);
    } catch (err) {
      if (err.name === 'AbortError' || seq !== cv.seq) return;
      showToast(err.message, false);
    } finally {
      if (seq === cv.seq) { $('#cv-grid').classList.remove('cv-loading'); $('#cv-more-btn').disabled = false; }
    }
  }
  function renderCatalog(reset) {
    $('#cv-count').textContent = t('items', { n: cv.total });
    $('#cv-grid').innerHTML = cv.items.map(prow).join('');
    if (reset) staggerOnce($('#cv-grid'));
    $('#cv-empty').hidden = cv.items.length > 0;
    var filtered = !!(cv.q || cv.brand);
    $('#cv-empty-p').textContent = t(filtered || route.cat !== 'all' ? 'f.none' : 'brand.empty');
    $('#cv-empty-reset').hidden = !filtered;
    $('#cv-more').hidden = cv.page + 1 >= cv.totalPages;
  }
  function resetFilters() { cv.q = ''; cv.brand = ''; renderCatHead(); loadCatalog(true); }

  var cvTimer = 0;
  $('#cv-q').addEventListener('input', function (e) {
    clearTimeout(cvTimer);
    var v = e.target.value.trim();
    cvTimer = setTimeout(function () {
      if (v === cv.q) return;
      cv.q = v;
      renderCatHead();
      $('#cv-q').focus();
      loadCatalog(true);
    }, DEBOUNCE_MS);
  });
  $('#cv-more-btn').addEventListener('click', function () { loadCatalog(false); });
  $('#f-reset').addEventListener('click', resetFilters);
  $('#cv-empty-reset').addEventListener('click', resetFilters);
  $('#f-toggle').addEventListener('click', function () { $('#cv-filters').classList.toggle('open'); });
  $('#f-panel').addEventListener('click', function (e) {
    var d = e.target.closest('[data-fx-del]');
    if (!d) return;
    if (d.getAttribute('data-fx-del') === 'q') cv.q = ''; else cv.brand = '';
    renderCatHead(); loadCatalog(true);
  });
  $('#br-row').addEventListener('click', function (e) {
    var b = e.target.closest('[data-fx-brand]'); if (!b) return;
    cv.brand = b.getAttribute('data-fx-brand');
    renderCatHead(); loadCatalog(true);
    var el = $('#br-row [data-fx-brand="' + cv.brand + '"]'); if (el) el.focus();
  });

  var pd = { id: null, img: 0, warranty: 0, product: null, ctrl: null, seq: 0 };
  function pdWarranty(p) { return warrantyYears(cart[p.id] ? cart[p.id].warranty : pd.warranty); }
  function pdPriceHTML(p) {
    var v = warrantyPrice(p.price, pdWarranty(p));
    return '<b class="num">' + money(v) + '</b>' + (STORE.usdRate ? '<span class="pd-uzs num">' + t('pd.uzs', { x: sum(uzsOf(v, STORE.usdRate)) }) + '</span>' : '');
  }
  function prodImages(p) {
    return (p.images || []).slice().sort(function (a, b) { return a.sortOrder - b.sortOrder; })
      .map(function (x) { return safeImg(x.url); }).filter(Boolean);
  }
  async function loadProduct(id) {
    var box = $('#prodview');
    if (pd.ctrl) pd.ctrl.abort();
    var ctrl = pd.ctrl = new AbortController(), seq = ++pd.seq;
    if (!pd.product || String(pd.product.id) !== String(id)) {
      box.innerHTML = '<div class="pd"><div class="skel pd-skel"></div><div class="skel pd-skel"></div></div>';
    }
    try {
      var p = await api('/api/products/' + encodeURIComponent(id), { signal: ctrl.signal });
      if (seq !== pd.seq) return;
      if (pd.id !== p.id) pd = { id: p.id, img: 0, warranty: WARRANTY[0].years, product: p, ctrl: pd.ctrl, seq: pd.seq };
      pd.product = p;
      known[p.id] = p;
      renderProduct();
      loadSimilar(p, seq);
    } catch (err) {
      if (err.name === 'AbortError' || seq !== pd.seq) return;
      pd.product = null;
      document.title = t('pd.notFound') + ' — E-Pay';
      box.innerHTML = '<div class="cv-empty pd-none"><svg class="ic"><use href="#i-box"/></svg><h1>'
        + esc(err.status === 404 ? t('pd.notFound') : err.message) + '</h1>'
        + (err.status === 404 ? '<p>' + t('pd.notFoundP') + '</p>' : '')
        + '<a class="btn btn-ink press" href="#/c/all">' + t('pd.toCatalog') + '</a></div>';
    }
  }
  async function loadSimilar(p, seq) {
    var catId = catIdOf(p); if (!catId) return;
    try {
      var data = await api('/api/products?categoryId=' + encodeURIComponent(catId) + '&size=5');
      if (seq !== pd.seq) return;
      var list = data.items.filter(function (x) { return x.id !== p.id; }).slice(0, 4);
      remember(list);
      var slot = $('#pd-similar');
      if (!slot || !list.length) return;
      slot.innerHTML = '<section class="pd-similar" aria-labelledby="pd-sim-h"><div class="sec-head">'
        + '<h2 id="pd-sim-h"><span>' + t('pd.similar') + '</span> <span class="sub">' + t('pd.similarSub') + '</span></h2>'
        + '<div class="rail-nav">'
        + '<button class="rail-btn press" type="button" data-rail="-1" aria-label="' + t('rail.prev') + '"><svg class="ic"><use href="#i-arrow"/></svg></button>'
        + '<button class="rail-btn press" type="button" data-rail="1" aria-label="' + t('rail.next') + '"><svg class="ic"><use href="#i-arrow"/></svg></button></div>'
        + '</div><div class="rail" id="pd-rail">' + list.map(card).join('') + '</div></section>';
      staggerOnce($('#pd-rail'));
    } catch (err) {
      return;
    }
  }
  function renderProduct() {
    var box = $('#prodview'), p = pd.product;
    if (!p) return;
    document.title = p.name + ' — E-Pay';
    var imgs = prodImages(p), i = Math.min(pd.img, Math.max(imgs.length - 1, 0)), sp = specs(p), keys = Object.keys(sp);
    var inCart = !!cart[p.id], w0 = WARRANTY[0].years, catId = catIdOf(p);
    var main = imgs.length
      ? '<img class="pimg" src="' + esc(imgs[i]) + '" alt="' + esc(p.name) + '" decoding="async">'
        + '<svg class="ill ill-lg" viewBox="0 0 64 64" aria-hidden="true"><use href="#' + ill(p) + '"/></svg>'
      : '<svg class="ill ill-lg" viewBox="0 0 64 64" aria-hidden="true"><use href="#' + ill(p) + '"/></svg>';
    var brandSlug = p.brand ? p.brand.slug : '';
    box.innerHTML = '<div class="pd-top">'
      + '<button class="pd-back press" type="button" data-pd="back"><svg class="ic"><use href="#i-arrow"/></svg>' + t('pd.back') + '</button>'
      + '<nav class="crumbs" aria-label="breadcrumb"><a href="#/">' + t('nav.home') + '</a><span>/</span>'
      + '<a href="#/c/' + esc(catId) + '">' + esc(tCat(catNameOf(p))) + '</a><span>/</span><span class="crumb-cur" aria-current="page">' + esc(p.name) + '</span></nav></div>'
      + '<div class="pd">'
      + '<div class="pd-media"><div class="pd-art' + (imgs.length ? ' has-img' : '') + '">' + main + '</div>'
      + (imgs.length > 1 ? '<div class="pd-thumbs">' + imgs.map(function (src, j) {
          return '<button class="pd-thumb has-img press" type="button" data-pd-img="' + j + '" aria-label="' + t('pd.photo', { n: j + 1 }) + '" aria-current="' + (j === i) + '">'
            + '<img class="pimg" src="' + esc(src) + '" alt="" loading="lazy" decoding="async"></button>';
        }).join('') + '</div>' : '')
      + '</div>'
      + '<div class="pd-info">'
      + '<h1 class="pd-name">' + esc(p.name) + '</h1>'
      + '<p class="pd-sku">' + esc(t('pd.sku', { x: p.sku })) + '</p>'
      + (p.brand ? '<p class="pd-brand">' + t('pd.brand') + ': <a href="#/b/' + esc(brandSlug) + '">' + esc(p.brand.name) + '</a></p>' : '')
      + '<p class="pd-price" id="pd-price">' + pdPriceHTML(p) + '</p>'
      + stockBadge(p)
      + '<label class="field pd-field">' + t('warranty.term')
      + '<select class="ci-warranty" id="pd-warranty">' + WARRANTY.map(function (w) {
          var extra = w.percent ? ' (+' + w.percent + '% · +' + money(warrantyPrice(p.price, w.years) - Number(p.price)) + ')' : ' (' + t('inPrice') + ')';
          return '<option value="' + w.years + '"' + (w.years === pdWarranty(p) ? ' selected' : '') + '>' + esc(t('warranty.n', { n: w.years, y: tYears(w.years) }) + extra) + '</option>';
        }).join('') + '</select></label>'
      + '<button class="btn btn-acc btn-wide add press pd-add" type="button" data-act="cart" data-id="' + esc(p.id) + '" data-label="pd.addCart" data-w="' + pdWarranty(p) + '" aria-pressed="' + inCart + '">'
      + '<svg class="ic"><use href="' + (inCart ? '#i-check' : '#i-bag') + '"/></svg><span>' + t(inCart ? 'cart.in' : 'pd.addCart') + '</span></button>'
      + (WARRANTY.length > 1 ? '<p class="dr-note">' + t('warranty.note', { n: w0, y: tYears(w0), x: warrantyRates() }) + '</p>' : '')
      + '</div></div>'
      + (p.description || keys.length ? '<div class="pd-more">'
        + (p.description ? '<section class="pd-block" aria-labelledby="pd-desc-h"><h2 id="pd-desc-h">' + t('pd.desc') + '</h2><p class="pd-desc">' + esc(p.description) + '</p></section>' : '')
        + (keys.length ? '<section class="pd-block" aria-labelledby="pd-specs-h"><h2 id="pd-specs-h">' + t('pd.specs') + '</h2><dl class="pd-specs">' + keys.map(function (k) {
            return '<div><dt>' + esc(tSpec(k)) + '</dt><dd>' + esc(sp[k]) + '</dd></div>';
          }).join('') + '</dl></section>' : '')
        + '</div>' : '')
      + '<div id="pd-similar"></div>';
  }
  $('#prodview').addEventListener('change', function (e) {
    if (e.target.id !== 'pd-warranty') return;
    var p = pd.product, w = Number(e.target.value); if (!p) return;
    pd.warranty = w;
    if (cart[p.id]) { cart[p.id].warranty = w; saveCart(); }
    $('#pd-price').innerHTML = pdPriceHTML(p);
    var b = $('#prodview .pd-add'); if (b) b.setAttribute('data-w', w);
  });
  $('#prodview').addEventListener('click', function (e) {
    var th = e.target.closest('[data-pd-img]');
    if (th) {
      pd.img = Number(th.getAttribute('data-pd-img'));
      var similar = $('#pd-similar') ? $('#pd-similar').innerHTML : '';
      renderProduct();
      $('#pd-similar').innerHTML = similar;
      $('#prodview [data-pd-img="' + pd.img + '"]').focus();
      return;
    }
    if (!e.target.closest('[data-pd="back"]')) return;
    var p = pd.product;
    if (visited.length > 1) history.back();
    else location.hash = p && catIdOf(p) ? '#/c/' + catIdOf(p) : '#/c/all';
  });

  var visited = [location.hash || '#/'];
  window.addEventListener('hashchange', function () {
    var h = location.hash || '#/';
    if (visited.length > 1 && visited[visited.length - 2] === h) visited.pop(); else visited.push(h);
    applyRoute();
  });
  document.addEventListener('click', function (e) { var a = e.target.closest('a[href="#"]'); if (a) e.preventDefault(); });

  var LAST_ORDER = 'lastOrder';
  var NUMBER_RE = /^EP-\d{4}-\d{6}$/;
  function rememberOrder(number, digits) { store(LAST_ORDER, { number: number, phone: digits }); }
  function normNumber(v) { return String(v || '').trim().toUpperCase(); }
  function openTrack(id) {
    var last = load(LAST_ORDER, {});
    var number = normNumber(id) || $('#ov-id').value || (last.number || '');
    $('#ov-id').value = number;
    if (!$('#ov-ph').value && last.phone && (!id || normNumber(id) === last.number)) $('#ov-ph').value = phoneMask(last.phone);
    $('#ov-result').hidden = true; $('#ov-form').hidden = false; $('#ov-err').hidden = true;
    if (NUMBER_RE.test(normNumber($('#ov-id').value)) && phoneDigits($('#ov-ph').value).length === 9) trackSubmit();
  }
  function trackLines(o) {
    return o.items.map(function (i) {
      return '<li><span>' + esc(i.productName) + ' × ' + esc(i.quantity)
        + '<small>' + esc(t('warranty.n', { n: i.warrantyYears, y: tYears(i.warrantyYears) })) + '</small></span>'
        + '<span class="num">' + money(i.lineTotalUsd) + '</span></li>';
    }).join('');
  }
  function fmtDate(iso) {
    var d = new Date(iso), p2 = function (n) { return String(n).padStart(2, '0'); };
    return LANG === 'uz'
      ? p2(d.getDate()) + '.' + p2(d.getMonth() + 1) + '.' + d.getFullYear() + ', ' + p2(d.getHours()) + ':' + p2(d.getMinutes())
      : d.toLocaleString(LANG === 'ru' ? 'ru-RU' : 'en-GB', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
  function orderCancelLink() { return ''; }
  function renderTrack(o) {
    $('#ov-result').innerHTML = '<div class="ov-card">'
      + '<div class="ov-top"><h2>' + esc(t('track.order', { id: o.number })) + '</h2>'
      + '<span class="st-pill st-' + esc(o.status) + '">' + esc(t('st.' + o.status)) + '</span></div>'
      + '<dl class="ov-meta">'
      + '<div><dt>' + t('track.date') + '</dt><dd>' + esc(fmtDate(o.createdAt)) + '</dd></div>'
      + '<div><dt>' + t('track.phone') + '</dt><dd class="num">' + esc(o.phoneMasked) + '</dd></div>'
      + '<div><dt>' + t('track.payment') + '</dt><dd>' + esc(payName(o.paymentMethod)) + ' · '
      + '<span class="pay-st pay-' + esc(o.payStatus) + '">' + esc(t('pay.' + o.payStatus)) + '</span></dd></div>'
      + '</dl>'
      + '<p class="f-title">' + t('track.items') + '</p><ul class="ov-items">' + trackLines(o) + '</ul>'
      + '<div class="sum"><span>' + t('total') + '</span><b>' + money(o.totalUsd) + '</b></div>'
      + '<div class="sum"><span>' + t('order.amountUzs') + '</span><b class="num">' + esc(t('uzs', { x: sum(o.amountUzs) })) + '</b></div>'
      + orderCancelLink(o)
      + '</div>'
      + '<button class="btn btn-ghost btn-wide press" type="button" id="ov-again">' + t('track.again') + '</button>';
    $('#ov-form').hidden = true; $('#ov-result').hidden = false;
    $('#ov-again').addEventListener('click', function () {
      $('#ov-result').hidden = true; $('#ov-form').hidden = false; $('#ov-id').value = ''; $('#ov-ph').value = ''; $('#ov-id').focus();
    });
  }
  var trackCtrl = null;
  async function trackSubmit() {
    var number = normNumber($('#ov-id').value), digits = phoneDigits($('#ov-ph').value);
    $('#ov-id').value = number;
    $('#ov-id-err').hidden = NUMBER_RE.test(number); $('#ov-ph-err').hidden = digits.length === 9; $('#ov-err').hidden = true;
    if (!NUMBER_RE.test(number)) { $('#ov-id').focus(); return; }
    if (digits.length !== 9) { $('#ov-ph').focus(); return; }
    if (trackCtrl) trackCtrl.abort();
    var ctrl = trackCtrl = new AbortController();
    var btn = $('#ov-submit');
    btn.disabled = true; btn.textContent = t('track.sending');
    try {
      var o = await api('/api/orders/track', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, signal: ctrl.signal,
        body: JSON.stringify({ number: number, phone: '+998' + digits })
      });
      rememberOrder(o.number, digits);
      renderTrack(o);
    } catch (err) {
      if (err.name === 'AbortError') return;
      $('#ov-err').textContent = err.status === 404 ? t('track.notFound') : err.status === 429 ? t('track.tooMany') : err.message;
      $('#ov-err').hidden = false;
    } finally {
      if (trackCtrl === ctrl) { btn.disabled = false; btn.textContent = t('track.submit'); }
    }
  }
  $('#ov-form').addEventListener('submit', function (e) { e.preventDefault(); trackSubmit(); });
  $('#ov-ph').addEventListener('input', function () { this.value = phoneMask(this.value); $('#ov-ph-err').hidden = true; });
  $('#ov-id').addEventListener('input', function () { this.value = this.value.toUpperCase().replace(/[^A-Z0-9-]/g, ''); $('#ov-id-err').hidden = true; });

  var toastTimer = 0;
  function showToast(msg, withCart) {
    var el = $('#toast');
    $('#toast-text').textContent = msg;
    el.querySelector('button').hidden = withCart === false;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('show'); }, 3200);
  }

  var cartEl = $('#cartview'), view = 'cart', lastOrder = null, undo = null;
  var form = { customerName: '', phone: '', region: '', address: '', pay: 'CASH' };
  function isCity() { return form.region === 'TOSHKENT_SHAHRI'; }

  var scrim = $('#scrim'), lastFocus = null;
  function openCart(v) {
    view = v || 'cart';
    lastFocus = document.activeElement;
    $('#toast').classList.remove('show');
    cartEl.hidden = false; scrim.hidden = false;
    renderCart();
    staggerOnce($('#cart-body'));
    requestAnimationFrame(function () { cartEl.classList.add('open'); scrim.classList.add('open'); $('#dr-close').focus(); });
    document.body.style.overflow = 'hidden';
  }
  function closeCart() {
    cartEl.classList.remove('open'); scrim.classList.remove('open');
    document.body.style.overflow = '';
    setTimeout(function () { cartEl.hidden = true; scrim.hidden = true; }, reduced ? 0 : 320);
    if (view === 'done') view = 'cart';
    if (lastFocus && lastFocus.focus) lastFocus.focus();
  }

  function warrantySelect(r) {
    return '<select class="ci-warranty" data-warranty="' + esc(r.id) + '" aria-label="' + t('warranty') + '">'
      + WARRANTY.map(function (w) {
          var extra = w.percent ? ' (+' + w.percent + '% · +' + money((unitPrice(r.price, w.years) - r.price) * r.qty) + ')' : ' (' + t('inPrice') + ')';
          return '<option value="' + w.years + '"' + (w.years === warrantyYears(r.warranty) ? ' selected' : '') + '>' + esc(t('warranty.n', { n: w.years, y: tYears(w.years) }) + extra) + '</option>';
        }).join('') + '</select>';
  }
  function thumb(k, cls) {
    var p = byId(k);
    return p ? '<a class="' + cls + (mainImage(p) ? ' has-img' : '') + '" href="' + prodHref(p) + '" tabindex="-1" aria-hidden="true">' + photo(p, 'ill-sm') + '</a>'
      : '<span class="' + cls + '" aria-hidden="true"><svg class="ic"><use href="#i-box"/></svg></span>';
  }
  function undoHTML() {
    return undo ? '<div class="cart-undo" role="status"><span>' + esc(t('cart.removed', { x: undo.row.name })) + '</span>'
      + '<button type="button" data-cart="undo">' + t('cart.undo') + '</button></div>' : '';
  }
  function totalBlock(btn) {
    var ship = view === 'checkout' && form.region ? t(isCity() ? 'co.etaCity' : 'co.etaReg') : t('cart.shipNote');
    var total = cartTotal();
    return '<div class="cart-total">'
      + '<div class="cart-total-row"><span>' + t('total') + '</span><b>' + money(total) + '</b></div>'
      + (STORE.usdRate ? '<p class="cart-ship num">' + esc(t('pd.uzs', { x: sum(uzsOf(total, STORE.usdRate)) })) + '</p>' : '')
      + '<p class="cart-ship"><svg class="ic" aria-hidden="true"><use href="#i-box"/></svg><span id="cart-ship">' + ship + '</span></p>'
      + btn + '</div>';
  }
  function regionOptions() {
    return '<option value="">' + t('co.regionPh') + '</option>' + REGIONS.map(function (r) {
      return '<option value="' + esc(r.code) + '"' + (r.code === form.region ? ' selected' : '') + '>' + esc(regionName(r)) + '</option>';
    }).join('');
  }
  function payOptions() {
    var methods = STORE.paymentMethods;
    if (methods.indexOf(form.pay) < 0) form.pay = methods[0];
    return '<div class="pays' + (methods.length > 2 ? ' three' : '') + '">' + methods.map(function (m) {
      return '<label class="pay"><input type="radio" name="pay" value="' + esc(m) + '"' + (form.pay === m ? ' checked' : '') + '><span>' + esc(payName(m)) + '</span></label>';
    }).join('') + '</div>';
  }

  function renderCart() {
    var body = $('#cart-body'), ids = cartIds();
    $('#cart-count').textContent = '';
    if (view === 'done') {
      var o = lastOrder;
      $('#cart-title').textContent = t('order');
      body.innerHTML = '<div class="done"><span class="ok-ring"><svg class="ic"><use href="#i-check"/></svg></span><h3>' + t('order.accepted') + '</h3>'
        + '<span class="order-no">' + esc(o.number) + '</span>'
        + '<dl class="done-sum">'
        + '<div><dt>' + t('order.totalUsd') + '</dt><dd class="num">' + money(o.totalUsd) + '</dd></div>'
        + '<div><dt>' + t('order.amountUzs') + '</dt><dd class="num">' + esc(t('uzs', { x: sum(o.amountUzs) })) + '</dd></div>'
        + '</dl>'
        + '<p>' + t('order.callSoon') + ' ' + t('order.payOnDelivery') + '</p>'
        + '<div class="done-btns"><a class="btn btn-acc press" href="#/order/' + encodeURIComponent(o.number) + '">' + t('order.trackHint') + '</a>'
        + '<a class="btn btn-ink press" href="#/">' + t('cart.keep') + '</a></div></div>';
      return;
    }
    if (!ids.length) {
      $('#cart-title').textContent = t('cart');
      body.innerHTML = undoHTML() + '<div class="cart-empty"><svg class="ic" aria-hidden="true"><use href="#i-bag"/></svg>'
        + '<h2>' + t('cart.empty') + '</h2><p>' + t('cart.emptyHint') + '</p>'
        + '<a class="btn btn-acc press" href="#/c/all">' + t('cart.toCatalog') + '</a></div>';
      return;
    }
    if (view === 'checkout') {
      $('#cart-title').textContent = t('order');
      body.innerHTML = '<button class="back" type="button" data-cart="back"><svg class="ic ic-back-sm"><use href="#i-arrow"/></svg>' + t('cart.back') + '</button>'
        + '<div class="co-form">'
        + '<label class="field">' + t('form.name') + '<input class="input" id="co-name" autocomplete="name" maxlength="100" placeholder="' + esc(t('form.namePh')) + '" value="' + esc(form.customerName) + '"><span class="err" id="name-err" hidden>' + t('form.nameErr') + '</span></label>'
        + '<label class="field">' + t('form.phone') + '<span class="phone"><span>+998</span><input id="ph" type="tel" inputmode="numeric" autocomplete="tel-national" placeholder="90 123 45 67" maxlength="12" value="' + esc(form.phone) + '"></span><span class="err" id="ph-err" hidden>' + t('form.phoneErr') + '</span></label>'
        + '<label class="field">' + t('co.region') + '<select class="input" id="co-region" required>' + regionOptions() + '</select><span class="err region-err" id="region-err" hidden>' + t('form.regionErr') + '</span></label>'
        + '<label class="field">' + t('form.addr') + '<input class="input" id="co-addr" autocomplete="street-address" maxlength="300" placeholder="' + esc(t('co.addrPh')) + '" value="' + esc(form.address) + '"><span class="err" id="addr-err" hidden>' + t('form.addrErr') + '</span></label>'
        + '<div class="field">' + t('pay.method') + payOptions() + '</div>'
        + '</div>'
        + '<p class="err" id="co-err" role="alert" hidden></p>'
        + totalBlock('<button class="btn btn-acc btn-wide press" type="button" data-cart="pay">' + payLabel() + '</button>');
      var ph = $('#ph');
      ph.addEventListener('input', function () {
        ph.value = phoneMask(ph.value);
        form.phone = ph.value;
        $('#ph-err').hidden = true;
      });
      $('#co-name').addEventListener('input', function (e) { form.customerName = e.target.value; $('#name-err').hidden = true; });
      $('#co-addr').addEventListener('input', function (e) { form.address = e.target.value; $('#addr-err').hidden = true; });
      $('#co-region').addEventListener('change', function (e) {
        form.region = e.target.value; $('#region-err').hidden = true;
        $('#cart-ship').textContent = form.region ? t(isCity() ? 'co.etaCity' : 'co.etaReg') : t('cart.shipNote');
      });
      body.querySelectorAll('input[name="pay"]').forEach(function (r) {
        r.addEventListener('change', function () { form.pay = r.value; body.querySelector('[data-cart="pay"]').textContent = payLabel(); });
      });
      return;
    }
    $('#cart-title').textContent = t('cart');
    $('#cart-count').textContent = t('items', { n: cartQty() });
    body.innerHTML = undoHTML() + '<div class="ci-list">' + ids.map(function (k) {
      var r = cart[k], q = r.qty, p = byId(k);
      return '<article class="ci">'
        + thumb(k, 'ci-img')
        + '<div class="ci-info">'
        + (p ? '<a class="ci-name" href="' + prodHref(p) + '">' + esc(r.name) + '</a>' : '<p class="ci-name">' + esc(r.name) + '</p>')
        + warrantySelect(r)
        + '</div>'
        + '<span class="qty"><button type="button" data-cart="dec" data-id="' + esc(k) + '" aria-label="' + t('cart.dec') + '">−</button><span>' + q + '</span>'
        + '<button type="button" data-cart="inc" data-id="' + esc(k) + '" aria-label="' + t('cart.inc') + '"' + (q >= MAX_QTY ? ' disabled' : '') + '>+</button></span>'
        + '<b class="ci-price">' + money(unitPrice(r.price, r.warranty) * q) + '</b>'
        + '<button class="ci-del" type="button" data-cart="del" data-id="' + esc(k) + '" aria-label="' + t('cart.remove') + '" title="' + t('cart.remove') + '"><svg class="ic"><use href="#i-x"/></svg></button>'
        + '</article>';
    }).join('') + '</div>'
      + totalBlock('<button class="btn btn-acc btn-wide press" type="button" data-cart="checkout">' + t('cart.checkout') + '<svg class="ic"><use href="#i-arrow"/></svg></button>');
  }
  function payLabel() {
    return t(form.pay === 'CASH' ? 'order.place' : 'order.pay', { x: money(cartTotal()) });
  }

  async function submitOrder(btn) {
    var digits = phoneDigits(form.phone);
    var bad = false;
    var name = form.customerName.trim(), address = form.address.trim();
    if (name.length < 2 || name.length > 100) { $('#name-err').hidden = false; bad = bad || $('#co-name'); }
    if (digits.length !== 9) { $('#ph-err').hidden = false; bad = bad || $('#ph'); }
    if (!form.region) { $('#region-err').hidden = false; bad = bad || $('#co-region'); }
    if (address.length < 5 || address.length > 300) { $('#addr-err').hidden = false; bad = bad || $('#co-addr'); }
    if (bad) { bad.focus(); return; }

    var items = cartIds().map(function (k) { var r = cart[k]; return { productId: r.id, quantity: r.qty, warrantyYears: warrantyYears(r.warranty) }; });
    btn.disabled = true; btn.textContent = t('order.sending');
    $('#co-err').hidden = true;
    try {
      var order = await api('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerName: name, phone: '+998' + digits, address: address, region: form.region, paymentMethod: form.pay, items: items })
      });
      rememberOrder(order.number, digits);
      cart = {}; undo = null; saveCart(); syncCartButtons(); updateCounts(true);
      lastOrder = order; view = 'done'; renderCart(); $('#cart-body').parentNode.scrollTop = 0;
    } catch (err) {
      btn.disabled = false; btn.textContent = payLabel();
      var msg = err.status === 429 ? t('err.429', { n: err.retryAfter || 60 }) : err.message;
      var e = $('#co-err'); if (e) { e.textContent = msg; e.hidden = false; }
    }
  }

  cartEl.addEventListener('change', function (e) {
    var w = e.target.closest('[data-warranty]'); if (!w) return;
    var r = cart[w.getAttribute('data-warranty')]; if (!r) return;
    r.warranty = Number(w.value); saveCart(); renderCart();
  });
  cartEl.addEventListener('click', function (e) {
    if (e.target.closest('a[href^="#"]')) { closeCart(); return; }
    var b = e.target.closest('[data-cart]'); if (!b) return;
    var a = b.getAttribute('data-cart'), id = b.getAttribute('data-id');
    if (a === 'pay') { submitOrder(b); return; }
    if (a === 'undo' && undo) { cart[undo.k] = undo.row; undo = null; }
    else undo = null;
    if (a === 'inc') { var r = cart[id]; r.qty = Math.min(MAX_QTY, r.qty + 1); }
    if (a === 'dec') {
      if (cart[id].qty <= 1) { undo = { k: id, row: Object.assign({}, cart[id], { qty: 1 }) }; delete cart[id]; }
      else cart[id].qty -= 1;
    }
    if (a === 'del') { undo = { k: id, row: cart[id] }; delete cart[id]; }
    if (a === 'checkout') view = 'checkout';
    if (a === 'back') view = 'cart';
    saveCart(); syncCartButtons(); updateCounts(false); renderCart();
    if (a === 'checkout' || a === 'back') $('#cart-body').parentNode.scrollTop = 0;
  });
  $('#dr-close').addEventListener('click', closeCart);
  scrim.addEventListener('click', closeCart);
  document.addEventListener('click', function (e) {
    var o = e.target.closest('[data-open="cart"]'); if (!o) return;
    e.preventDefault(); openCart();
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !cartEl.hidden) closeCart();
    if (e.key === 'Tab' && !cartEl.hidden) {
      var f = cartEl.querySelectorAll('button:not([disabled]), input, select, [href]');
      if (!f.length) return;
      var first = f[0], last = f[f.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
  });

  async function validateCart() {
    var ids = Object.keys(cart);
    if (!ids.length) return;
    var gone = [];
    await Promise.all(ids.map(function (k) {
      return api('/api/products/' + encodeURIComponent(k)).then(function (p) {
        known[p.id] = p;
        cart[k].price = Number(p.price); cart[k].name = p.name;
        if (cart[k].qty > MAX_QTY) cart[k].qty = MAX_QTY;
      }).catch(function (err) {
        if (err.status === 404) { gone.push(cart[k].name); delete cart[k]; }
      });
    }));
    saveCart(); syncCartButtons(); updateCounts(false);
    if (!cartEl.hidden) renderCart();
    if (gone.length) showToast(gone.map(function (n) { return t('cart.gone', { x: n }); }).join(' · '), false);
  }

  var q = $('#q'), sug = $('#suggest'), sel = -1, hits = [], sugTimer = 0, sugCtrl = null, sugSeq = 0;
  function norm(s) { return String(s || '').toLowerCase(); }
  function hl(text, term) {
    var i = norm(text).indexOf(norm(term));
    if (!term || i < 0) return esc(text);
    return esc(text.slice(0, i)) + '<mark>' + esc(text.slice(i, i + term.length)) + '</mark>' + esc(text.slice(i + term.length));
  }
  function renderSuggest(term) {
    sel = hits.length ? 0 : -1;
    sug.innerHTML = hits.length ? hits.map(function (p, i) {
      return '<div class="sug" role="option" data-pid="' + esc(p.id) + '" aria-selected="' + (i === sel) + '">'
        + '<span class="sug-txt"><span class="sug-brand">' + esc(metaLine(p)) + '</span><span class="sug-name">' + hl(p.name, term) + '</span></span>'
        + '<span class="sug-price">' + money(p.price) + '</span></div>';
    }).join('') : '<div class="sug-none">' + t('search.none') + '</div>';
    sug.hidden = false; q.setAttribute('aria-expanded', 'true');
  }
  function runSearch() {
    clearTimeout(sugTimer);
    var term = q.value.trim();
    if (!term) {
      if (sugCtrl) sugCtrl.abort();
      sugSeq++;
      sug.hidden = true; q.setAttribute('aria-expanded', 'false'); return;
    }
    sugTimer = setTimeout(async function () {
      if (sugCtrl) sugCtrl.abort();
      var ctrl = sugCtrl = new AbortController(), seq = ++sugSeq;
      try {
        var data = await api('/api/products?q=' + encodeURIComponent(term) + '&size=8', { signal: ctrl.signal });
        if (seq !== sugSeq) return;
        hits = data.items; remember(hits);
        renderSuggest(term);
      } catch (err) {
        if (err.name === 'AbortError' || seq !== sugSeq) return;
        hits = []; sug.innerHTML = '<div class="sug-none">' + esc(err.message) + '</div>'; sug.hidden = false;
      }
    }, DEBOUNCE_MS);
  }
  function goProduct(id) {
    sug.hidden = true; q.setAttribute('aria-expanded', 'false'); q.blur();
    setSearch(false);
    location.hash = '#/p/' + encodeURIComponent(id);
  }
  function goCatalogSearch(term) {
    sug.hidden = true; q.setAttribute('aria-expanded', 'false'); q.blur();
    setSearch(false);
    cv.q = term; cv.brand = '';
    if (location.hash === '#/c/all') applyRoute(); else location.hash = '#/c/all';
  }
  q.addEventListener('input', runSearch);
  q.addEventListener('focus', function () { if (q.value.trim()) runSearch(); });
  q.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (!sug.hidden && sel >= 0 && hits[sel]) goProduct(hits[sel].id);
      else if (q.value.trim()) goCatalogSearch(q.value.trim());
      return;
    }
    if (sug.hidden || !hits.length) return;
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault(); sel = (sel + (e.key === 'ArrowDown' ? 1 : -1) + hits.length) % hits.length;
      sug.querySelectorAll('.sug').forEach(function (x, i) { x.setAttribute('aria-selected', String(i === sel)); });
    }
    if (e.key === 'Escape') { sug.hidden = true; }
  });
  sug.addEventListener('mousedown', function (e) { var x = e.target.closest('[data-pid]'); if (x) { e.preventDefault(); goProduct(x.getAttribute('data-pid')); } });
  document.addEventListener('click', function (e) { if (!e.target.closest('.search')) { sug.hidden = true; q.setAttribute('aria-expanded', 'false'); } });
  document.addEventListener('keydown', function (e) {
    if (e.key === '/' && !/INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName)) { e.preventDefault(); setSearch(true); }
  });

  var hdr = $('#hdr'), searchPanel = $('#search-panel'), searchBtn = $('#search-btn'), menuBtn = $('#menu-btn');
  function setSearch(open) {
    searchPanel.hidden = !open;
    searchBtn.setAttribute('aria-expanded', String(open));
    hdr.classList.toggle('search-open', open);
    if (open) { setMenu(false); q.focus(); } else { sug.hidden = true; q.setAttribute('aria-expanded', 'false'); }
  }
  function setMenu(open) {
    hdr.classList.toggle('menu-open', open);
    menuBtn.setAttribute('aria-expanded', String(open));
    document.body.classList.toggle('menu-lock', open);
    if (open) setSearch(false);
  }
  searchBtn.addEventListener('click', function () { setSearch(searchPanel.hidden); });
  menuBtn.addEventListener('click', function () { setMenu(!hdr.classList.contains('menu-open')); });
  $('#hdr-nav').addEventListener('click', function (e) { if (e.target.closest('a')) setMenu(false); });
  document.addEventListener('click', function (e) {
    if (!searchPanel.hidden && !e.target.closest('#search-panel, #search-btn')) setSearch(false);
  });
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    if (!searchPanel.hidden) { setSearch(false); searchBtn.focus(); }
    if (hdr.classList.contains('menu-open')) { setMenu(false); menuBtn.focus(); }
  });
  window.addEventListener('resize', function () { if (window.innerWidth >= 1000) setMenu(false); });
  document.addEventListener('click', function (e) {
    var h = e.target.closest('[data-help]'); if (!h) return;
    e.preventDefault();
    $('#help').scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'center' });
  });
  document.addEventListener('click', function (e) {
    var b = e.target.closest('[data-rail]'); if (!b) return;
    var rail = b.closest('section').querySelector('.rail');
    rail.scrollBy({ left: Number(b.getAttribute('data-rail')) * rail.clientWidth * 0.85, behavior: reduced ? 'auto' : 'smooth' });
  });

  function renderWarrantyFact() {
    var max = WARRANTY[WARRANTY.length - 1].years, base = WARRANTY[0].years;
    $('#fact-warr-h').textContent = t('fact.warrH', { y: tYears(max) });
    $('#fact-warr-p').textContent = t('fact.warrP', { n: base, y: tYears(base), x: warrantyRates() });
  }

  (function () {
    var tabs = [].slice.call(document.querySelectorAll('[data-ab-tab]'));
    if (!tabs.length) return;
    function setOn(id) {
      tabs.forEach(function (b) {
        var on = b.getAttribute('data-ab-tab') === id;
        b.classList.toggle('is-on', on);
        var nav = b.parentNode;
        if (on && nav.scrollWidth > nav.clientWidth) nav.scrollLeft = b.offsetLeft - (nav.clientWidth - b.offsetWidth) / 2;
      });
    }
    tabs.forEach(function (b) {
      b.addEventListener('click', function () {
        var el = document.getElementById(b.getAttribute('data-ab-tab'));
        if (el) el.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'start' });
      });
    });
    setOn(tabs[0].getAttribute('data-ab-tab'));
    if (!('IntersectionObserver' in window)) return;
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { if (e.isIntersecting) setOn(e.target.id); });
    }, { rootMargin: '-30% 0px -60% 0px' });
    tabs.forEach(function (b) { var el = document.getElementById(b.getAttribute('data-ab-tab')); if (el) io.observe(el); });
  })();

  function skeleton() {
    var s = '';
    for (var i = 0; i < 4; i++) s += '<div class="skel"></div>';
    $('#featured').innerHTML = s;
  }

  async function loadShop() {
    var data = await Promise.all([
      api('/api/categories'),
      api('/api/products?size=' + HOME_SIZE),
      api('/api/brands').catch(function () { return []; })
    ]);
    categories = data[0]; home = data[1].items; homeTotal = data[1].totalElements; brands = data[2];
    homeLoaded = true;
    remember(home);
    renderHome();
    if (route.name === 'cat') { renderCatHead(); loadCatalog(true); document.title = viewName() + ' — E-Pay'; }
    updateCounts(false);
  }

  renderWarrantyFact();
  loadStorefront().then(function () {
    renderWarrantyFact();
    if (route.name === 'prod' && pd.product) renderProduct();
    if (!cartEl.hidden) renderCart();
  }).catch(function () {});
  loadRegions().then(function () { if (!cartEl.hidden && view === 'checkout') renderCart(); }).catch(function () {});

  skeleton();
  updateCounts(false);
  applyRoute(false);
  validateCart();
  loadShop().then(function () {
    var params = new URLSearchParams(location.search);
    if (params.has('cart')) openCart();
    if (params.toString()) history.replaceState(null, '', location.pathname + location.hash);
  }).catch(function (err) {
    $('#featured').innerHTML = '';
    showToast(err.message || t('loadErr'), false);
  });
})();
