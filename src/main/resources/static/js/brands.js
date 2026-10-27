(function () {
  var $ = function (s) { return document.querySelector(s); };
  var brands = [], counts = {}, letter = '';

  function logo(b) {
    var src = safeImg(b.logo);
    return src ? '<img src="' + esc(src) + '" alt="' + esc(b.name) + '" data-brand-logo loading="lazy" decoding="async">'
      : '<span class="partner-ph">' + esc(b.name) + '</span>';
  }
  document.addEventListener('error', function (e) {
    var img = e.target;
    if (!img || img.tagName !== 'IMG' || !img.hasAttribute('data-brand-logo')) return;
    var s = document.createElement('span');
    s.className = 'partner-ph';
    s.textContent = img.alt;
    img.replaceWith(s);
  }, true);

  function card(b) {
    var n = counts[b.id];
    var meta = [b.country, b.founded ? t('brand.since', { y: b.founded }) : ''].filter(Boolean).join(' · ');
    var site = safeHttps(b.website);
    var href = '/#/b/' + encodeURIComponent(b.slug);
    return '<article class="bcard" id="b-' + esc(b.slug) + '">'
      + '<a class="brand-logo bcard-logo" href="' + esc(href) + '" tabindex="-1" aria-hidden="true">' + logo(b) + '</a>'
      + '<div class="bcard-body">'
      + '<h2><a href="' + esc(href) + '">' + esc(b.name) + '</a></h2>'
      + (meta ? '<p class="bcard-meta">' + esc(meta) + '</p>' : '')
      + (b.description ? '<p class="bcard-desc">' + esc(b.description) + '</p>' : '')
      + '<div class="bcard-links">'
      + '<a class="btn btn-acc press" href="' + esc(href) + '">' + esc(n == null ? t('nav.catalog') : t('brand.toProducts', { n: n })) + '<svg class="ic"><use href="#i-arrow"/></svg></a>'
      + (site ? '<a class="link-more" href="' + esc(site) + '" target="_blank" rel="noopener noreferrer">' + t('brand.site')
        + ' <svg class="ic"><use href="#i-ext"/></svg></a>' : '')
      + '</div></div></article>';
  }

  function render() {
    var have = {};
    brands.forEach(function (b) { have[b.name.charAt(0).toUpperCase()] = true; });
    if (letter && !have[letter]) letter = '';
    $('#bv-letters').innerHTML = '<button type="button" data-letter="" aria-pressed="' + !letter + '">' + t('brands.all') + '</button>'
      + 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').map(function (l) {
          return '<button type="button" data-letter="' + l + '" aria-pressed="' + (l === letter) + '"' + (have[l] ? '' : ' disabled') + '>' + l + '</button>';
        }).join('');
    var list = brands.filter(function (b) { return !letter || b.name.charAt(0).toUpperCase() === letter; });
    $('#bv-count').textContent = t('brands.n', { n: list.length });
    $('#bcards').innerHTML = list.map(card).join('');
  }

  $('#bv-letters').addEventListener('click', function (e) {
    var b = e.target.closest('[data-letter]'); if (!b || b.disabled) return;
    letter = b.getAttribute('data-letter');
    render();
    staggerOnce($('#bcards'));
  });

  $('#bcards').innerHTML = '<div class="skel bcard-skel"></div><div class="skel bcard-skel"></div>';
  api('/api/brands').then(function (list) {
    brands = list;
    render();
    staggerOnce($('#bcards'));
    var target = location.hash && document.getElementById('b-' + location.hash.slice(1));
    if (target) target.scrollIntoView({ block: 'center' });
    return Promise.all(brands.map(function (b) {
      return api('/api/products?brandId=' + encodeURIComponent(b.id) + '&size=1')
        .then(function (page) { counts[b.id] = page.totalElements; })
        .catch(function () {});
    })).then(render);
  }).catch(function (err) {
    $('#bcards').innerHTML = '<p class="cv-empty">' + esc(err.message || t('loadErr')) + '</p>';
  });
})();
