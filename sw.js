// Atlas — service worker : appli disponible hors ligne + tuiles des secteurs téléchargés
const SHELL='atlas-shell-v1',DATA='atlas-data';
const LEAF=['https://unpkg.com/leaflet@1.9.4/dist/leaflet.js','https://unpkg.com/leaflet@1.9.4/dist/leaflet.css','https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png','https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png','https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png'];
const BASE=['./','index.html','manifest.json','icon-192.png','icon-512.png','lock.json'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(SHELL).then(c=>Promise.all([...BASE,...LEAF].map(u=>fetch(u,{cache:'reload'}).then(r=>{if(r.ok)return c.put(u,r)}).catch(()=>{})))).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(n=>n.startsWith('atlas-shell-')&&n!==SHELL).map(n=>caches.delete(n)))).then(()=>self.clients.claim()))});
const enCache=req=>caches.match(req,{ignoreVary:true});
async function reseauPuisCache(req,nom,cle){try{const r=await fetch(req);if(r.ok){const c=await caches.open(nom);c.put(cle||req,r.clone())}return r}catch(e){const m=await caches.match(cle||req,{ignoreVary:true,ignoreSearch:!!cle});if(m)return m;throw e}}
self.addEventListener('fetch',e=>{const req=e.request;if(req.method!=='GET')return;const u=new URL(req.url);
 if(u.origin===location.origin){
  if(req.mode==='navigate'||u.pathname.endsWith('/index.html')||u.pathname.endsWith('/'))return e.respondWith(reseauPuisCache(req,SHELL,new URL('index.html',self.registration.scope).href));
  if(u.pathname.includes('/t/')){
   if(req.cache==='no-cache'||req.cache==='no-store')return e.respondWith(reseauPuisCache(req,DATA));          // plans.json : toujours la dernière version, copie gardée
   return e.respondWith(enCache(req).then(m=>m||fetch(req).catch(()=>new Response('',{status:404}))))}       // tuiles des plans : copie locale d'abord
  return e.respondWith(enCache(req).then(m=>m||fetch(req)))}
 if(/api\.github\.com|raw\.githubusercontent\.com|nominatim|apicarto/.test(u.host))return;                      // partage / recherche : réseau seulement
 e.respondWith(enCache(req).then(m=>m||fetch(req).catch(()=>new Response('',{status:504}))))});                // fonds de carte : copie locale d'abord
