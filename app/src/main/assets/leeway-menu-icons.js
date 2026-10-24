/* LeeWay A2 Android menu: semantic vector symbols, no emoji or font glyph placeholders. */
(()=>{'use strict';
const paths={
'digital-brain':'<path d="M12 5a4 4 0 0 0-7 3 4 4 0 0 0-1 7 4 4 0 0 0 4 6h1V6m3-1a4 4 0 0 1 7 3 4 4 0 0 1 1 7 4 4 0 0 1-4 6h-1V6M5 11h3m8 0h3M6 16h3m6 0h3M9 8v9m6-9v9"/>',
'continuum':'<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 4 16 4 16 0V5M4 12c0 4 16 4 16 0"/>',
'diagnostics':'<path d="M10 2h4l.7 2.7 2.1.9L19.2 4 22 7l-1.6 2.4.9 2.1L24 12v4l-2.7.7-.9 2.1L22 21l-2.8 2-2.4-1.6-2.1.9L14 25h-4l-.7-2.7-2.1-.9L4.8 23 2 20l1.6-2.4-.9-2.1L0 15v-4l2.7-.7.9-2.1L2 6l2.8-2 2.4 1.6 2.1-.9z" transform="translate(2 1) scale(.83)"/><circle cx="12" cy="13" r="3.5"/>',
'voice':'<rect x="9" y="2" width="6" height="13" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v4m-4 0h8"/>',
'workstation':'<path d="m12 2 9 5v10l-9 5-9-5V7zM3 7l9 5 9-5M12 12v10"/>',
'models':'<path d="M12 4a4 4 0 0 0-6 4 4 4 0 0 0-1 7 4 4 0 0 0 5 6h2V4zm0 0a4 4 0 0 1 6 4 4 4 0 0 1 1 7 4 4 0 0 1-5 6h-2M8 10h3m-3 5h3m2-5h3m-3 5h3"/>',
'skills':'<path d="M4 7h16M4 12h16M4 17h16"/><circle cx="7" cy="7" r="1.5"/><circle cx="7" cy="12" r="1.5"/><circle cx="7" cy="17" r="1.5"/>',
'authority':'<path d="M12 2 21 6v6c0 5-4 8-9 10C7 20 3 17 3 12V6z"/><path d="m8 12 3 3 5-6"/>',
'evidence':'<path d="M3 7V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
'about':'<circle cx="12" cy="12" r="10"/><path d="M12 11v6M12 7h.01"/>'
};
function install(){
 document.querySelectorAll('#drawer button.element-card.setting-card[data-setting]').forEach(btn=>{
 const key=btn.dataset.setting;if(!paths[key]||btn.querySelector('.lw-menu-icon'))return;
 const holder=document.createElement('span');holder.className='lw-menu-icon';holder.setAttribute('aria-hidden','true');
 holder.innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="#47d4ff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="38" height="38">'+paths[key]+'</svg>';
 btn.prepend(holder);
 });
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install);else install();
})();
