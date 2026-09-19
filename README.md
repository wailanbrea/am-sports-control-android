# A&M Sports Control Android

Aplicación Android para administrar bancas, cobros, adelantos, caja chica, libro mayor y cuadres semanales.

## API

La configuración de producción apunta a:

```text
https://amsport.bsolutions.dev/api/v1/
```

## Cambios actuales

- Inicio de sesión contra Laravel Sanctum.
- Gestión de bancas y consulta de saldos.
- Registro de cobros y adelantos.
- Consulta de libro mayor y caja chica.
- Nuevo formulario de cuadre semanal desde el detalle de cada banca.
- Cálculo en vivo de `ventas - premios + efectivo entregado`.
- Historial de cuadres con saldo anterior, balance semanal y saldo posterior.

## Verificación local

```powershell
cmd /c gradlew.bat test assembleRelease
```

El APK release se genera en:

```text
app/build/outputs/apk/release/app-release.apk
```

## Pendiente de producción

El cliente Android ya incluye las llamadas a `weekly-settlements`, pero el endpoint requiere que el backend ejecute la migración `2026_09_19_100000_create_weekly_settlements_table`. En el VPS esa migración está pendiente porque la cuenta de aplicación no tiene permiso `CREATE`; debe resolverse con una cuenta administrativa o permisos DDL temporales antes de usar el formulario en producción.

No se deben versionar APKs, tokens, contraseñas, archivos `.env`, bases SQLite ni claves privadas.
