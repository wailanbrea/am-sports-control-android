# Prompt de despliegue de A&M Sports Control

Actúa como administrador senior de despliegues Windows, Laravel, Apache y MariaDB.

Objetivo: dejar operativo A&M Sports Control en producción, sin tocar otros sistemas del VPS.

## Datos del proyecto

Repositorio backend:

```text
https://github.com/wailanbrea/am-sports-control-api.git
```

Repositorio Android:

```text
https://github.com/wailanbrea/am-sports-control-android.git
```

Dominio:

```text
https://amsport.api.bsolutions.dev
```

VPS:

- Acceso administrativo por el alias SSH `bsolutions-vps`.
- Nunca uses `62.171.174.191` para SSH, RDP o administración.
- Proyecto remoto: `C:\xampp\htdocs\amsport-api`
- Apache: `C:\xampp\apache`
- Vhosts: `C:\xampp\apache\conf\extra\httpd-vhosts.conf`
- Certificado existente: `C:\xampp\apache\conf\ssl.crt\server.crt`
- Llave existente: `C:\xampp\apache\conf\ssl.key\server.key`

No tocar:

- `C:\xampp\htdocs\btm-api-piloto`
- `C:\xampp\php\www\PosBill`
- `C:\xampp\htdocs\bslottery`
- `walletFinanzas`
- Ninguna base de datos existente
- Ningún DNS ajeno a `amsport.api.bsolutions.dev`

## 1. Verificar el estado actual

Ejecuta:

```powershell
ssh bsolutions-vps "hostname"
git -C C:\xampp\htdocs\amsport-api status
git -C C:\xampp\htdocs\amsport-api log -1 --oneline
```

Si el proyecto no existe, clónalo:

```powershell
ssh bsolutions-vps "git clone --branch main https://github.com/wailanbrea/am-sports-control-api.git C:\xampp\htdocs\amsport-api"
```

Si ya existe, actualízalo sin sobrescribir cambios locales:

```powershell
ssh bsolutions-vps "git -C C:\xampp\htdocs\amsport-api pull --ff-only origin main"
```

## 2. Crear una base de datos exclusiva

Usa una cuenta administrativa de MariaDB mediante el mecanismo seguro del VPS. No escribas contraseñas en comandos, archivos Git, historial ni salida de consola.

Crea una base exclusiva para el proyecto:

```sql
CREATE DATABASE IF NOT EXISTS amsport_api
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'amsport_api_app'@'localhost'
  IDENTIFIED BY '<GENERAR_CONTRASENA_UNICA_Y_FUERTE>';

GRANT SELECT, INSERT, UPDATE, DELETE
  ON amsport_api.* TO 'amsport_api_app'@'localhost';

FLUSH PRIVILEGES;
```

La contraseña real debe configurarse directamente en el servidor y nunca mostrarse.

## 3. Instalar el backend

En `C:\xampp\htdocs\amsport-api` ejecuta:

```powershell
composer install --no-dev --optimize-autoloader
```

Configura `.env` en el VPS, sin subirlo a Git:

```text
APP_NAME="A&M Sports Control API"
APP_ENV=production
APP_DEBUG=false
APP_URL=https://amsport.api.bsolutions.dev

APP_LOCALE=en
APP_FALLBACK_LOCALE=en

DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=amsport_api
DB_USERNAME=amsport_api_app
DB_PASSWORD=<VALOR_SEGURO_NO_MOSTRAR>

SESSION_DRIVER=file
CACHE_STORE=file
QUEUE_CONNECTION=sync
```

Conserva el `APP_KEY` existente si ya fue generado. Si no existe, genera uno:

```powershell
php C:\xampp\htdocs\amsport-api\artisan key:generate --force
```

Nunca ejecutes `key:generate` sobre una instalación productiva ya configurada.

## 4. Ejecutar migraciones y datos demo

Ejecuta únicamente sobre la base nueva `amsport_api`:

```powershell
php C:\xampp\htdocs\amsport-api\artisan migrate --seed --force
php C:\xampp\htdocs\amsport-api\artisan optimize
```

No uses `migrate:fresh` en producción.

El `DatabaseSeeder` debe ejecutar `DemoSeeder`, que crea:

- Empresa: `A & M Sports LLC`
- Moneda: USD
- Usuario demo definido actualmente en `C:\xampp\php\www\BTMContabilidadApi\database\seeders\DemoSeeder.php`
- 10 bancas
- Cobros de dos semanas
- 3 adelantos
- Libro mayor
- Movimientos de caja

No cambies los datos ni la contraseña del usuario demo del `DemoSeeder`.

## 5. Crear el usuario Antonio

Crea el usuario:

```text
Nombre: Antonio
Correo: antonio@gmail.com
Contraseña: <USAR_AQUÍ_LA_CLAVE_SOLICITADA_POR_EL_PROPIETARIO>
```

La contraseña debe introducirse mediante un mecanismo seguro y no debe aparecer en historial, logs, código, Git ni en la respuesta final.

Usa Laravel `Hash::make`, no guardes la contraseña en texto plano.

Asocia a Antonio como usuario `admin`, con estado `active`, en la empresa:

```text
A & M Sports LLC
```

Verifica que ambos usuarios tengan membresía activa en `company_user`.

## 6. Configurar Apache

Edita como administrador:

```text
C:\xampp\apache\conf\extra\httpd-vhosts.conf
```

Agrega un vhost HTTP que redirija a HTTPS:

```apache
<VirtualHost *:80>
    ServerName amsport.api.bsolutions.dev

    DocumentRoot "C:/xampp/htdocs/amsport-api/public"

    <Directory "C:/xampp/htdocs/amsport-api/public">
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    RewriteEngine On
    RewriteCond %{HTTP:X-Forwarded-Proto} !https [NC]
    RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [R=301,L]

    ErrorLog "logs/amsport-api-http-error.log"
    CustomLog "logs/amsport-api-http-access.log" common
</VirtualHost>
```

Agrega un vhost HTTPS:

```apache
<VirtualHost *:443>
    ServerName amsport.api.bsolutions.dev

    SSLEngine on
    SSLCertificateFile "conf/ssl.crt/server.crt"
    SSLCertificateKeyFile "conf/ssl.key/server.key"

    DocumentRoot "C:/xampp/htdocs/amsport-api/public"

    <Directory "C:/xampp/htdocs/amsport-api/public">
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    SetEnvIfNoCase X-Forwarded-Proto "^https$" HTTPS=on
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"

    ErrorLog "logs/amsport-api-ssl-error.log"
    CustomLog "logs/amsport-api-ssl-access.log" common
</VirtualHost>
```

No reemplaces el archivo completo. Conserva todos los vhosts existentes.

## 7. Validar y recargar Apache

Como administrador:

```powershell
C:\xampp\apache\bin\httpd.exe -t
```

Si la sintaxis es correcta, recarga Apache según el mecanismo instalado en el VPS. No reinicies servicios desde una cuenta sin privilegios.

## 8. Verificar la API

Comprueba:

```powershell
curl.exe -i https://amsport.api.bsolutions.dev/up
```

Debe devolver HTTP 200 desde el proyecto A&M Sports Control, no desde PosBill.

Comprueba el login sin mostrar el token:

```powershell
curl.exe -sS -o NUL -w "%{http_code}" `
  -X POST https://amsport.api.bsolutions.dev/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"antonio@gmail.com","password":"<INTRODUCIR_DE_FORMA_SEGURA>","device_name":"verification"}'
```

Debe devolver HTTP 200.

Verifica también que una contraseña incorrecta devuelva HTTP 422 o 401.

## 9. Verificar Android

El APK release del repositorio Android debe usar:

```text
https://amsport.api.bsolutions.dev/api/v1/
```

El APK debug puede seguir usando la URL LAN de desarrollo.

No subas APKs, `.env`, bases SQLite, tokens, claves privadas ni contraseñas a GitHub.

## 10. Reporte final

Entrega únicamente:

- Commit desplegado.
- Ruta del backend.
- Resultado de `httpd.exe -t`.
- Estado HTTP de `/up`.
- Confirmación de que la base `amsport_api` existe.
- Número de tablas migradas.
- Confirmación de que el usuario demo existe.
- Confirmación de que `antonio@gmail.com` existe como administrador activo.
- Confirmación de que la API responde por HTTPS.

No muestres:

- Contraseñas.
- `APP_KEY`.
- `DB_PASSWORD`.
- Tokens Sanctum.
- Claves privadas.
- Contenido completo de `.env`.

Si no tienes privilegios para crear la base, modificar Apache o recargar el servicio, detente y reporta exactamente qué paso quedó bloqueado. No simules éxito ni modifiques otros proyectos.
