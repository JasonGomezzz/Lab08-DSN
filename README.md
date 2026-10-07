# TechStore · Sistema de gestión de inventario con autenticación segura

Proyecto del Laboratorio 08 de Desarrollo de Soluciones en la Nube (seguridad en la nube). Es una aplicación web para una cadena de tiendas de tecnología con cuatro perfiles de usuario y estos controles:

- **Parte 1:** registro con política de contraseñas, login con JWT, bloqueo tras 5 intentos fallidos y login con **Google** y **GitHub**.
- **Parte 2:** autenticación multifactor con **TOTP** (Google Authenticator o similar): código de 6 dígitos que cambia cada 30 segundos, máximo 3 intentos.
- **Autorización:** permisos por perfil y por tienda, validados siempre en el servidor.

## Arquitectura

```
Navegador ──► Spring Boot :8080 ──► MySQL 8.4
              ├─ /            interfaz React (dentro del jar)
              ├─ /api/**      API REST con JWT
              └─ /oauth2/**   inicio y retorno de Google y GitHub
```

La interfaz React se compila dentro de la imagen y la sirve el mismo jar, así que todo corre en un único contenedor más la base de datos.

### Flujo de autenticación

1. `POST /api/auth/login` valida correo y contraseña. Si son correctos **no** entrega el token completo: devuelve un **token MFA parcial** de 5 minutos que solo sirve para el segundo factor.
2. Primer inicio: `POST /api/auth/mfa/enrolar` genera el secreto TOTP y la URI `otpauth://` para el QR. Inicios siguientes: se pide el código directamente.
3. `POST /api/auth/mfa/verificar` con el código de 6 dígitos. Si es correcto devuelve el **JWT completo** (1 hora). Si falla, quedan menos intentos; al tercer fallo el desafío se anula y hay que volver a iniciar sesión.
4. Google y GitHub entran por el mismo camino: tras el consentimiento el servidor crea o reconoce la cuenta y redirige a la interfaz con el token MFA parcial en el fragmento de la URL (no llega a registros del servidor). Después se pide el mismo código TOTP.

## Requisitos

- **Con Docker (recomendado):** Docker con Docker Compose. No hace falta instalar Java ni Node.
- **Para desarrollar:** JDK 21, Node 22 y un MySQL 8.4 (puede ser un contenedor).
- **Para las pruebas del backend:** Docker en ejecución (usan Testcontainers con MySQL real).

## Instalación y ejecución

### Con Docker

```bash
git clone https://github.com/JasonGomezzz/Lab08-DSN.git
cd Lab08-DSN
cp .env.example .env        # reemplaza todos los valores CAMBIAR (ver "Configuración")
docker compose up -d --build
```

Abre <http://localhost:8080>. Para detener y borrar los datos: `docker compose down -v`.

Con `TECHSTORE_DATOS_DEMO=true` se cargan además un usuario por perfil y 13 productos de ejemplo (contraseña: la de `DEMO_PASSWORD`):

| Correo | Perfil | Tienda |
|---|---|---|
| `ADMIN_EMAIL` | Administrador | todas |
| `gerente.lima@techstore.demo` | Gerente de tienda | Lima Centro |
| `ventas.lima@techstore.demo` | Empleado de ventas | Lima Centro |
| `gerente.arequipa@techstore.demo` | Gerente de tienda | Arequipa |
| `ventas.arequipa@techstore.demo` | Empleado de ventas | Arequipa |
| `auditor@techstore.demo` | Auditor | todas (solo lectura) |

### En desarrollo

```bash
# 1. MySQL
docker run -d --name techstore-mysql -p 3307:3306 \
  -e MYSQL_DATABASE=techstore -e MYSQL_USER=techstore -e MYSQL_PASSWORD=<clave> \
  -e MYSQL_ROOT_PASSWORD=<clave-root> mysql:8.4

# 2. Backend (carga las variables de .env y apunta a ese MySQL)
cd backend
set -a && . ../.env && set +a
DB_URL=jdbc:mysql://localhost:3307/techstore TECHSTORE_FRONTEND_URL=http://localhost:5173 ./mvnw spring-boot:run

# 3. Interfaz (en otra terminal; reenvía /api y /oauth2 al backend)
cd frontend
npm install
npm run dev                 # http://localhost:5173
```

## Configuración

Todas las variables están en [`.env.example`](.env.example) con valores de mentira; el `.env` real no se versiona. Genera secretos con `openssl rand -base64 48`. La aplicación se niega a arrancar si `JWT_SECRET` o `MFA_CLAVE_CIFRADO` conservan el marcador `CAMBIAR…` del ejemplo.

| Variable | Para qué sirve |
|---|---|
| `DB_USER`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Credenciales de MySQL |
| `JWT_SECRET` | Firma de los JWT (mínimo 32 bytes) |
| `MFA_CLAVE_CIFRADO` | Clave con la que se cifran en reposo los secretos TOTP (mínimo 32 bytes, distinta de `JWT_SECRET`) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Administrador inicial. Si faltan no se crea ninguno; la contraseña debe cumplir la política |
| `TECHSTORE_DATOS_DEMO`, `DEMO_PASSWORD` | Datos de demostración (solo en local) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Habilitan el login con Google |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | Habilitan el login con GitHub |
| `TECHSTORE_BASE_URL`, `TECHSTORE_FRONTEND_URL` | URL pública del backend (para el redirect de OAuth2) y de la interfaz |

### Login con Google y GitHub

Sin credenciales la aplicación arranca igual y simplemente no muestra esos botones (`GET /api/auth/proveedores` lista los habilitados).

- **Google:** en Google Cloud Console crea una pantalla de consentimiento y un *ID de cliente OAuth* de tipo *Aplicación web*. URI de redireccionamiento autorizado: `<TECHSTORE_BASE_URL>/login/oauth2/code/google`. Alcances: `profile` y `email`.
- **GitHub:** en *Settings → Developer settings → OAuth Apps* crea una app. *Authorization callback URL*: `<TECHSTORE_BASE_URL>/login/oauth2/code/github`. Alcances: `read:user` y `user:email`.

Copia el Client ID y el Client Secret al `.env` y reinicia el backend. En local, `TECHSTORE_BASE_URL` es `http://localhost:8080`.

## Estructura

```
backend/                      Spring Boot 3.5 · Java 21 · Maven
  src/main/java/com/techstore/inventario/
    autenticacion/            registro, login, JWT, TOTP, MFA, bloqueo, Google y GitHub, seguridad
    autorizacion/             matriz de permisos por rol y por tienda
    productos/                inventario, ajuste atómico de stock y reportes
    usuarios/                 entidad de usuario y administración
    tiendas/                  tiendas
    comun/                    errores, propiedades, datos iniciales, tareas programadas
  src/main/resources/db/migration/   migraciones Flyway
  src/test/                   pruebas con MySQL real (Testcontainers)
frontend/                     React 19 · Vite · Tailwind 4 · TypeScript
Dockerfile · docker-compose.yml
```

## Perfiles y permisos

| Operación | Administrador | Gerente de tienda | Empleado de ventas | Auditor |
|---|:-:|:-:|:-:|:-:|
| Ver productos | todas las tiendas | su tienda | su tienda | todas las tiendas |
| Crear productos | sí | su tienda | no | no |
| Editar datos y precio | sí | su tienda | no | no |
| Actualizar stock | sí | su tienda | su tienda | no |
| Eliminar productos | sí | su tienda | no | no |
| Reporte de inventario | todas | su tienda | no | todas |
| Ver usuarios | sí | no | no | sí |
| Cambiar roles, tiendas, desbloquear, reiniciar MFA | sí | no | no | no |

## API

| Método y ruta | Acceso |
|---|---|
| `POST /api/auth/registro` | público |
| `POST /api/auth/login` | público |
| `POST /api/auth/mfa/enrolar`, `POST /api/auth/mfa/verificar` | token MFA parcial |
| `POST /api/auth/logout`, `GET /api/auth/me`, `PUT /api/auth/me/tienda` | sesión completa |
| `GET /api/auth/proveedores`, `GET /api/tiendas`, `GET /api/actuator/health` | público |
| `GET /api/productos[?tiendaId=]`, `GET /api/productos/{id}` | `VER_PRODUCTOS` |
| `POST /api/productos` | `CREAR_PRODUCTO` |
| `PUT /api/productos/{id}` | `EDITAR_PRODUCTO` |
| `PATCH /api/productos/{id}/stock` (cuerpo `{"ajuste": -1}`) | `ACTUALIZAR_STOCK` |
| `DELETE /api/productos/{id}` | `ELIMINAR_PRODUCTO` |
| `GET /api/reportes/inventario` | `VER_REPORTE` |
| `GET /api/usuarios` | `VER_USUARIOS` |
| `PATCH /api/usuarios/{id}/rol`, `PATCH /api/usuarios/{id}/tienda`, `POST /api/usuarios/{id}/desbloquear`, `POST /api/usuarios/{id}/reiniciar-mfa` | `GESTIONAR_USUARIOS` |

Los errores usan `application/problem+json` con un campo `codigo` estable (`CREDENCIALES_INVALIDAS`, `CUENTA_BLOQUEADA`, `MFA_CODIGO_INVALIDO`, `MFA_INTENTOS_AGOTADOS`, `TIENDA_AJENA`, `ROL_SIN_PERMISO`, etc.).

Pantallas de la interfaz: `/login`, `/registro`, `/mfa`, `/tienda`, `/inventario`, `/reportes`, `/usuarios`.

## Decisiones de seguridad

- Contraseñas con **BCrypt** (coste 12); la política (8 a 64 caracteres, mayúscula, número y carácter especial) se valida en el servidor. Nunca se devuelve un hash.
- **JWT HS256** con dos tipos de token (`MFA` de 5 minutos y `ACCESO` de 1 hora). El cierre de sesión revoca el token por su `jti`.
- **Bloqueo:** 5 fallos seguidos bloquean la cuenta 15 minutos; el contador se reinicia solo al completar el segundo factor. Agotar los 3 intentos de un código cuenta como un fallo, para que no se pueda forzar el código repitiendo el login.
- **TOTP** según RFC 6238 (HMAC-SHA1, 6 dígitos, 30 s) con tolerancia de ±1 paso y rechazo de códigos ya usados. El secreto se guarda **cifrado con AES-256-GCM** atado a su usuario.
- Un usuario con MFA activo no puede volver a enrolarse solo con la contraseña; un administrador puede reiniciarlo.
- Login y registro responden igual para correos inexistentes y contraseñas incorrectas (no revelan cuentas) y gastan el mismo tiempo de cómputo.
- El stock se ajusta con una sola sentencia atómica (`stock + ajuste >= 0`): ventas simultáneas nunca lo dejan negativo.
- La interfaz guarda el token en `sessionStorage` (se borra al cerrar la pestaña). JavaScript de la página puede leerlo, por eso la autorización real vive en el servidor y la interfaz solo decide qué mostrar.

## Cómo probar

```bash
cd backend && ./mvnw test        # más de 180 pruebas; requieren Docker
cd frontend && npm run build     # comprueba los tipos y compila la interfaz
docker compose config -q         # valida el compose
```

Las pruebas del backend arrancan un MySQL real y cubren registro, política de contraseñas, bloqueo con reloj controlable, TOTP (con los vectores oficiales del RFC 6238), cifrado, JWT, flujo MFA completo, login social contra un proveedor OAuth2 simulado, la matriz de permisos por rol y por tienda, y la concurrencia del stock.

Para probar el segundo factor a mano, escanea el QR de `/mfa` con Google Authenticator, o calcula el código a partir de la clave que se muestra bajo el QR con cualquier generador TOTP (SHA-1, 6 dígitos, 30 s).

## Desviaciones y supuestos respecto al enunciado

- **Pila tecnológica:** el enunciado no la fija; se usó la misma de los laboratorios anteriores del curso (Spring Boot, MySQL, React, Docker).
- **Inventario mínimo:** el enunciado solo detalla autenticación, pero describe cuatro perfiles con restricciones sobre productos. Se implementó un inventario básico (productos por tienda, stock, precio, reporte) para que esas reglas se puedan demostrar.
- **Duración del bloqueo:** el enunciado fija 5 intentos pero no cuánto dura; se eligió 15 minutos, con desbloqueo manual por un administrador.
- **Rol al registrarse:** el registro público siempre crea un *Empleado de ventas*; los demás perfiles los asigna un administrador (el administrador inicial sale de variables de entorno).
- **Login social y MFA:** las cuentas de Google y GitHub también pasan por el código TOTP. Entran sin tienda y la eligen una sola vez en `/tienda`.
- **Sin fusión de cuentas:** una cuenta social nunca se une a una cuenta local con el mismo correo, porque el registro con contraseña no verifica el correo y la fusión permitiría adueñarse de una cuenta ajena. En ese caso se pide entrar con la contraseña.
- **Correo de GitHub:** se toma solo de `/user/emails` (principal y verificado), no del perfil público.
- **Moneda:** los importes se muestran en soles (S/); el enunciado no indica moneda.
- **Alcance no incluido:** despliegue en AWS, recuperación de contraseña, envío de correos y límite de peticiones por IP.

## Repositorio

<https://github.com/JasonGomezzz/Lab08-DSN>
