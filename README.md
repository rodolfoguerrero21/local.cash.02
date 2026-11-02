# Local Cash

Local Cash es una aplicación Android nativa desarrollada en Java para administrar finanzas personales. Permite registrar cuentas, ingresos, egresos, compras, contactos, deudas compartidas y ubicaciones de interés desde una interfaz adaptada para teléfonos y tablets.

## Funciones principales

- Acceso protegido mediante `BiometricPrompt` y el método de seguridad configurado en el dispositivo.
- Creación y eliminación de cuentas de efectivo, ahorro o tarjeta.
- Registro de ingresos y egresos asociados a una cuenta.
- Ubicación opcional en los movimientos financieros.
- Registro de compras con uno o más productos o servicios.
- Compras realizadas o programadas para una fecha futura, descontadas inmediatamente del saldo disponible.
- Clasificación de compras por alimentación, transporte, estudios o gastos personales.
- Asociación de contactos a compras compartidas.
- Aportes personalizados por participante en compras compartidas.
- Descuento automático de la diferencia entre el total y los aportes de los participantes.
- Listado de compras compartidas con el nombre y aporte de cada participante.
- Resumen de presupuesto, total gastado, saldo disponible y gastos por categoría.
- Mapa interactivo con MapLibre, ubicación del dispositivo y puntos personalizados.
- Creación de puntos manteniendo presionado el mapa, asignación de nombres y eliminación posterior.
- Persistencia local mediante SQLite.

## Tecnologías

- Java 11
- Android SDK 37
- SQLite con `SQLiteOpenHelper`
- AndroidX y Material Components
- AndroidX Biometric
- MapLibre Android SDK
- Gradle Wrapper

## Requisitos

- Android Studio compatible con Android Gradle Plugin 9.3.3.
- JDK 11 o superior.
- Android SDK 37 instalado.
- Dispositivo o emulador con Android 8.0 (API 26) o superior.
- Biometría, PIN, patrón o contraseña configurada en el dispositivo.
- Conexión a Internet para cargar las teselas visuales del mapa.

Los datos financieros se almacenan localmente en SQLite. La conexión se utiliza únicamente para cargar el estilo y las teselas cartográficas de MapLibre.

## Compilación

Desde PowerShell, en la carpeta raíz del proyecto:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Instalación mediante ADB

Con un dispositivo conectado y la depuración USB activada:

```powershell
adb devices
adb install -r -t -g app\build\outputs\apk\debug\app-debug.apk
```

## Uso básico

1. Abrir Local Cash y validar la identidad.
2. Crear una cuenta e indicar su saldo inicial.
3. Registrar ingresos, egresos o compras.
4. Agregar contactos para dividir compras compartidas.
5. Consultar el resumen financiero y los gastos por categoría.
6. Abrir el mapa para revisar o crear puntos personalizados.

## Estructura principal

```text
app/src/main/java/com/example/clase/
├── Database/DatabaseHelper.java
├── MainActivity.java
├── AccountsActivity.java
├── TransactionsActivity.java
├── PurchaseFormActivity.java
├── PurchasesActivity.java
├── ContactsActivity.java
├── DebtsActivity.java
├── MapActivity.java
├── MapPointsActivity.java
├── modelo/
└── ui/
```

## Base de datos

La base `localcash.db` contiene las tablas principales:

- `cuentas`
- `contactos`
- `movimientos`
- `compras`
- `productos_compra`
- `deudas`
- `puntos_mapa`

Las operaciones financieras relacionadas se ejecutan mediante transacciones y claves foráneas para mantener la integridad de los datos.

## Pruebas

El proyecto incluye pruebas unitarias y una prueba instrumentada del flujo financiero local en:

```text
app/src/androidTest/java/com/example/clase/LocalCashDatabaseTest.java
```

Estas pruebas comprueban cuentas, movimientos, compras futuras, aportes personalizados, categorías, saldos compartidos y puntos del mapa utilizando una base SQLite temporal.
