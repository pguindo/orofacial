// sw.js - Service Worker para Entrenador Orofacial
self.addEventListener('install', (e) => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));

// EVENTO FETCH OBLIGATORIO PARA QUE ANDROID CREE EL WEBAPK (APP DEL SISTEMA)
self.addEventListener('fetch', (event) => {
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request))
  );
});

let reminderConfig = { enabled: false, time: '20:00', lastDate: '', completedToday: false };

self.addEventListener('message', (e) => {
  if (e.data && e.data.type === 'SYNC_CONFIG') {
    reminderConfig = Object.assign(reminderConfig, e.data.config);
  }
});

async function checkBackgroundReminder() {
  if (!reminderConfig.enabled || reminderConfig.completedToday) return;

  const now = new Date();
  const todayStr = [now.getFullYear(), String(now.getMonth()+1).padStart(2,'0'), String(now.getDate()).padStart(2,'0')].join('-');
  if (reminderConfig.lastDate === todayStr) return;

  const [tH, tM] = (reminderConfig.time || '20:00').split(':').map(Number);
  const nowMin = now.getHours() * 60 + now.getMinutes();
  const targetMin = tH * 60 + tM;

  if (nowMin >= targetMin) {
    reminderConfig.lastDate = todayStr;
    await self.registration.showNotification('¡Momento de tu entrenamiento orofacial! 🦷✨', {
      body: 'Has alcanzado tu hora límite diaria. Dedica solo 3 minutos a cuidar tu musculatura orofacial. ¡Tú puedes!',
      icon: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"%3E%3Crect width="512" height="512" rx="128" fill="%234F46E5"/%3E%3Ccircle cx="256" cy="205" r="42" fill="%2384E5CE"/%3E%3C/svg%3E',
      tag: 'orofacial-daily-reminder',
      renotify: true,
      vibrate: [200, 100, 200]
    });
  }
}

self.addEventListener('periodicsync', (e) => {
  if (e.tag === 'daily-orofacial-check') {
    e.waitUntil(checkBackgroundReminder());
  }
});

self.addEventListener('sync', (e) => {
  if (e.tag === 'daily-orofacial-check') {
    e.waitUntil(checkBackgroundReminder());
  }
});

self.addEventListener('notificationclick', (e) => {
  e.notification.close();
  e.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url && 'focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow('./');
    })
  );
});
