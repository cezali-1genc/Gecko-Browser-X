(function () {
  if (window.__modluShieldApplied) return;
  window.__modluShieldApplied = true;
  const style = document.createElement('style');
  style.textContent = `
    [id*="ad-banner"], [class*="ad-banner"], [class*="sponsored-card"],
    ins.adsbygoogle, iframe[src*="doubleclick.net"], iframe[src*="googlesyndication.com"] {
      display: none !important;
    }
  `;
  (document.head || document.documentElement).appendChild(style);
})();
