(function () {
  'use strict';

  var PARTNERS = [
    { name: 'Hewlett Packard Enterprise', slug: 'hpe',      logo: 'hpe.svg' },
    { name: 'Dell', slug: 'dell',                           logo: 'dell.svg', scale: 1.45 },
    { name: 'Lenovo', slug: 'lenovo',                       logo: 'lenovo.svg' },
    { name: 'VMware', slug: 'vmware',                       logo: 'vmware.svg' },
    { name: 'Microsoft', slug: 'microsoft',                 logo: 'microsoft.svg' },
    { name: 'Cisco', slug: 'cisco',                         logo: 'cisco.svg' },
    { name: 'Veeam', slug: 'veeam',                         logo: 'veeam.svg', scale: 0.7 },
    { name: 'Palo Alto Networks', slug: 'paloalto',         logo: 'paloalto.svg' }
  ];
  var LOGO_DIR = '/images/brands/';
  var COMPANY_LOGO = '';
  var COMPANY_NAME = 'Smart Server LLC';
  var SECONDS_PER_LOGO = 3.5;

  function item(p, hidden, cls) {
    var el = document.createElement(p.slug ? 'a' : 'div');
    el.className = hidden ? cls + ' dup' : cls;
    if (p.slug) {
      el.href = '/#/b/' + p.slug;
      if (hidden) el.tabIndex = -1;
    }
    if (p.scale) el.style.setProperty('--s', p.scale);
    var img = document.createElement('img');
    img.src = LOGO_DIR + p.logo;
    img.alt = hidden ? '' : p.name + ' logo';
    img.loading = 'lazy';
    img.decoding = 'async';
    img.addEventListener('error', function () {
      var s = document.createElement('span');
      s.className = 'partner-ph';
      s.textContent = p.name;
      img.replaceWith(s);
    });
    el.appendChild(img);
    return el;
  }

  function fill(track, cls) {
    if (!track) return;
    for (var half = 0; half < 2; half++) {
      var group = document.createElement('div');
      group.className = 'marquee-group';
      if (half) group.setAttribute('aria-hidden', 'true');
      for (var rep = 0; rep < 2; rep++) {
        PARTNERS.forEach(function (p) { group.appendChild(item(p, half > 0 || rep > 0, cls)); });
      }
      track.appendChild(group);
    }
    track.style.setProperty('--marquee-dur', (PARTNERS.length * 2 * SECONDS_PER_LOGO) + 's');
  }
  fill(document.getElementById('partners-track'), 'partner');

  var company = document.getElementById('partners-company');
  if (company && COMPANY_LOGO) {
    var logo = new Image();
    logo.onload = function () {
      logo.className = 'pc-logo';
      logo.alt = COMPANY_NAME + ' logo';
      company.classList.add('has-logo');
      company.insertBefore(logo, company.firstChild);
    };
    logo.src = COMPANY_LOGO;
  }
})();
