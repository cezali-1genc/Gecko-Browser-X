(function () {
  if (window.__modluNightApplied) return;
  window.__modluNightApplied = true;
  const style = document.createElement('style');
  style.textContent = `
    html {
      color-scheme: dark !important;
    }
  `;
  (document.head || document.documentElement).appendChild(style);
})();
