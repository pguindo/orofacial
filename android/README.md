# App Android — Entrenador Orofacial

Proyecto mínimo en Kotlin (`applicationId com.pguindo.orofacial`):

- `MainActivity`: WebView a pantalla completa que carga `https://pguindo.github.io/orofacial/`
  y expone `window.Android.guardarDato(json)` al JS de `index.html`.
- Widget clásico (`OrofacialWidgetProvider` + `RemoteViews`) que lee
  `SharedPreferences "orofacial_widget"` y muestra contento / inquieto / lloroso / derrotado.
- `OrofacialWidgetWorker` (WorkManager, cada 15 min) para transiciones de hora
  (deadline, 22:00, medianoche) con la app cerrada.

## Lógica de estados (`WidgetData.calcularEstado`)

1. Hecha hoy → **contento**.
2. Si no, `hora >= 22:00` → **lloroso** (gana al deadline).
3. Si no, `hora >= deadline` → **inquieto**.
4. Si no (mañana con margen): ayer fallado (`lastDate < ayer`) → **derrotado**, si no → **contento**.

El JS solo envía datos brutos (`{sessionsToday, totalSessions, lastDate, deadline}`);
Kotlin decide la emoción.

## Sustituir las imágenes del personaje

Reemplaza estos 4 vectores placeholder manteniendo el nombre:

- `app/src/main/res/drawable/ic_mood_contento.xml`
- `app/src/main/res/drawable/ic_mood_inquieto.xml`
- `app/src/main/res/drawable/ic_mood_lloroso.xml`
- `app/src/main/res/drawable/ic_mood_derrotado.xml`

Puedes poner PNGs en `drawable-nodpi/` con el mismo nombre base
(`ic_mood_contento.png`, …) y borrar el `.xml`.

## Compilar

Local (Android Studio o línea de comandos con JDK 17 + SDK):

```powershell
cd android
gradle :app:assembleRelease
# APK en app/build/outputs/apk/release/app-release.apk
```

La `release` se firma con el keystore propio `keystore/orofacial.jks` (generado una vez
por CI y versionado en el repo): todas las releases comparten firma y actualizar es
descargar la APK nueva e instalarla encima, conservando datos. Sin ese fichero
(p. ej. compilación local) se usa la clave `debug`. Para Play Store, sustituye el
keystore y fija `OROFACIAL_KS_PASS` como secreto.

> Nota: hasta v1.0.3 se firmaba con claves efímeras del runner, así que para pasar a
> v1.0.4 hay que desinstalar una última vez (exporta antes tu JSON desde Historial)
> y luego instalar limpio e importar. Desde v1.0.4 las actualizaciones son encima.

## Publicar la APK en GitHub Releases

1. `git tag v1.0.2; git push origin v1.0.2`
2. El workflow `.github/workflows/android-apk.yml` compila, renombra a
   `OrofacialCoach-vX.Y.Z.apk` y lo adjunta a la release.
3. La web (`index.html`) sugiere esa APK con el banner `#nativeBanner`
   cuando se abre en Android fuera de la app nativa (resuelve el asset .apk
   de la última release vía API de GitHub).

## Probar el puente JS

Con la app abierta, en `chrome://inspect` o consola del WebView:

```js
Android.guardarDato(JSON.stringify({sessionsToday:1,totalSessions:5,lastDate:"2026-09-26",deadline:"20:00"}))
```

Luego añade el widget "Orofacial hoy" a la pantalla de inicio.
