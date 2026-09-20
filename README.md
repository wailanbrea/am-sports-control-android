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
- Comisión editable con `20%` predeterminado y cálculo automático del monto.
- Cálculo en vivo de `ventas - premios - comisión + efectivo entregado`.
- Historial de cuadres con saldo anterior, balance semanal y saldo posterior.

## Verificación local

```powershell
cmd /c gradlew.bat test assembleRelease
```

El APK release se genera en:

```text
app/build/outputs/apk/release/app-release.apk
```

## Estado de producción

El cliente Android ya incluye las llamadas a `weekly-settlements` y el backend ya tiene ejecutada la migración `2026_09_19_100000_create_weekly_settlements_table` en producción. El APK release todavía debe instalarse o distribuirse por el canal elegido.

Pendientes funcionales: definir si se requiere capturar el detalle de cada número jugado y su premio individual, ya que el cuadre actual registra los totales semanales auditables.

No se deben versionar APKs, tokens, contraseñas, archivos `.env`, bases SQLite ni claves privadas.
