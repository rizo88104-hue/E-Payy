(function () {
  var ORDER = ['auto', 'light', 'dark'];
  var tr = typeof t === 'function' ? t : function (k) { return { 'theme': 'Mavzu', 'theme.auto': 'avto', 'theme.light': 'yorug‘', 'theme.dark': 'qorong‘i' }[k]; };
  var LABEL = { auto: tr('theme.auto'), light: tr('theme.light'), dark: tr('theme.dark') };
  var theme = 'auto';
  try { theme = localStorage.getItem('theme') || 'auto'; } catch (e) {}
  if (ORDER.indexOf(theme) < 0) theme = 'auto';

  function apply() {
    var root = document.documentElement;
    if (theme === 'auto') root.removeAttribute('data-theme'); else root.setAttribute('data-theme', theme);
    var btn = document.getElementById('theme-mini');
    if (btn) { btn.setAttribute('aria-label', tr('theme') + ': ' + LABEL[theme]); btn.title = tr('theme') + ': ' + LABEL[theme]; }
  }
  apply();

  var btn = document.getElementById('theme-mini');
  if (btn) btn.addEventListener('click', function () {
    theme = ORDER[(ORDER.indexOf(theme) + 1) % ORDER.length];
    try { localStorage.setItem('theme', theme); } catch (e) {}
    apply();
  });

  var hdr = document.getElementById('hdr');
  if (hdr) {
    var onScroll = function () { hdr.classList.toggle('scrolled', window.scrollY > 4); };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }
})();
