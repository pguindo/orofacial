// sw.js retirado: los avisos push se eliminaron (el widget los sustituye).
// Este stub solo sirve para desregistrar el SW antiguo en navegadores que lo
// tuvieran instalado. Cuando todos los clientes estén limpios, borrar el fichero
// y quitar su referencia en index.html (solo queda unregisterLegacyServiceWorker).
self.addEventListener('install', (e) => self.skipWaiting());
self.addEventListener('activate', (e) => {
  e.waitUntil(
    self.registration.unregister().then(() => self.clients.claim())
  );
});
