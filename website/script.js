// script.js — small interactions
const modal = document.getElementById('modal');
const openDemo = document.getElementById('openDemo');
const modalClose = document.getElementById('modalClose');

openDemo && openDemo.addEventListener('click', () => {
  modal.setAttribute('aria-hidden', 'false');
});

modalClose && modalClose.addEventListener('click', () => {
  modal.setAttribute('aria-hidden', 'true');
});

// close on backdrop click
modal && modal.addEventListener('click', (e) => {
  if (e.target === modal) modal.setAttribute('aria-hidden','true');
});

// keyboard: ESC closes modal
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') modal.setAttribute('aria-hidden','true');
});

// set year
document.getElementById('year').textContent = new Date().getFullYear();

// Replace this with your repo releases URL
const GITHUB_RELEASES_URL = 'https://github.com/defiphoenix007/LabelScan/releases/download/v1.0.0/app-release.apk';
document.getElementById('downloadPrimary').setAttribute('href', GITHUB_RELEASES_URL);
document.querySelectorAll('.download-badge, .download-hero').forEach(a => a.setAttribute('href', GITHUB_RELEASES_URL));

// Smooth scrolling for internal nav
document.querySelectorAll('a.nav-link').forEach(a => {
  a.addEventListener('click', (e) => {
    e.preventDefault();
    const id = a.getAttribute('href').slice(1);
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({behavior:'smooth', block:'start'});
  });
});
