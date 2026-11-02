package com.example.clase.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.clase.modelo.Compra;
import com.example.clase.modelo.Contacto;
import com.example.clase.modelo.Cuenta;
import com.example.clase.modelo.Deuda;
import com.example.clase.modelo.Movimiento;
import com.example.clase.modelo.Producto;
import com.example.clase.modelo.ProductoCompra;
import com.example.clase.modelo.PuntoMapa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TIPO_INGRESO = "INGRESO";
    public static final String TIPO_EGRESO = "EGRESO";

    private static final String DATABASE_NAME = "localcash.db";
    private static final int DATABASE_VERSION = 4;

    public DatabaseHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    public DatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE compras ADD COLUMN categoria TEXT NOT NULL " +
                    "DEFAULT 'Gastos Personales / Estilo de Vida'");
        }
        createSchema(db);
    }

    private void createSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, cantidad INTEGER NOT NULL, precio REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cuentas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, " +
                "tipo TEXT NOT NULL, saldo REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS contactos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, " +
                "telefono TEXT, email TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS compras (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, descripcion TEXT NOT NULL, " +
                "fecha INTEGER NOT NULL, programada INTEGER NOT NULL DEFAULT 0, " +
                "cuenta_id INTEGER, total REAL NOT NULL, categoria TEXT NOT NULL, " +
                "FOREIGN KEY(cuenta_id) REFERENCES cuentas(id) ON DELETE RESTRICT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS productos_compra (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER NOT NULL, " +
                "nombre TEXT NOT NULL, cantidad INTEGER NOT NULL, precio_unitario REAL NOT NULL, " +
                "FOREIGN KEY(compra_id) REFERENCES compras(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS movimientos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta_id INTEGER NOT NULL, " +
                "tipo TEXT NOT NULL, monto REAL NOT NULL, descripcion TEXT NOT NULL, " +
                "fecha INTEGER NOT NULL, latitud REAL, longitud REAL, compra_id INTEGER, " +
                "FOREIGN KEY(cuenta_id) REFERENCES cuentas(id) ON DELETE RESTRICT, " +
                "FOREIGN KEY(compra_id) REFERENCES compras(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS deudas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER NOT NULL, " +
                "contacto_id INTEGER NOT NULL, monto REAL NOT NULL, pagada INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(compra_id) REFERENCES compras(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(contacto_id) REFERENCES contactos(id) ON DELETE RESTRICT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS puntos_mapa (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, " +
                "latitud REAL NOT NULL, longitud REAL NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta ON movimientos(cuenta_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos(fecha)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_deudas_compra ON deudas(compra_id)");
    }

    public long insertCuenta(String nombre, String tipo, double saldoInicial) {
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("tipo", tipo);
        values.put("saldo", saldoInicial);
        return getWritableDatabase().insert("cuentas", null, values);
    }

    public List<Cuenta> getCuentas() {
        List<Cuenta> cuentas = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "cuentas", null, null, null, null, null, "nombre COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                cuentas.add(new Cuenta(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("saldo"))));
            }
        }
        return cuentas;
    }

    public boolean deleteCuenta(long id) {
        return getWritableDatabase().delete("cuentas", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public long insertContacto(String nombre, String telefono, String email) {
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("telefono", telefono);
        values.put("email", email);
        return getWritableDatabase().insert("contactos", null, values);
    }

    public List<Contacto> getContactos() {
        List<Contacto> contactos = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "contactos", null, null, null, null, null, "nombre COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                contactos.add(new Contacto(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email"))));
            }
        }
        return contactos;
    }

    public boolean deleteContacto(long id) {
        return getWritableDatabase().delete("contactos", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public long insertMovimiento(long cuentaId, String tipo, double monto, String descripcion,
                                 long fecha, Double latitud, Double longitud) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long movementId = insertMovimientoInternal(
                    db, cuentaId, tipo, monto, descripcion, fecha, latitud, longitud, null);
            if (movementId == -1) return -1;
            adjustBalance(db, cuentaId, TIPO_INGRESO.equals(tipo) ? monto : -monto);
            db.setTransactionSuccessful();
            return movementId;
        } finally {
            db.endTransaction();
        }
    }

    private long insertMovimientoInternal(SQLiteDatabase db, long cuentaId, String tipo, double monto,
                                           String descripcion, long fecha, Double latitud,
                                           Double longitud, Long compraId) {
        ContentValues values = new ContentValues();
        values.put("cuenta_id", cuentaId);
        values.put("tipo", tipo);
        values.put("monto", monto);
        values.put("descripcion", descripcion);
        values.put("fecha", fecha);
        if (latitud == null) values.putNull("latitud"); else values.put("latitud", latitud);
        if (longitud == null) values.putNull("longitud"); else values.put("longitud", longitud);
        if (compraId == null) values.putNull("compra_id"); else values.put("compra_id", compraId);
        return db.insert("movimientos", null, values);
    }

    private void adjustBalance(SQLiteDatabase db, long cuentaId, double delta) {
        db.execSQL("UPDATE cuentas SET saldo = saldo + ? WHERE id = ?", new Object[]{delta, cuentaId});
    }

    public List<Movimiento> getMovimientos() {
        List<Movimiento> movimientos = new ArrayList<>();
        String sql = "SELECT m.*, c.nombre AS cuenta_nombre FROM movimientos m " +
                "JOIN cuentas c ON c.id=m.cuenta_id ORDER BY m.fecha DESC, m.id DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                int latitudeIndex = cursor.getColumnIndexOrThrow("latitud");
                int longitudeIndex = cursor.getColumnIndexOrThrow("longitud");
                movimientos.add(new Movimiento(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("cuenta_nombre")),
                        cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("monto")),
                        cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("fecha")),
                        cursor.isNull(latitudeIndex) ? null : cursor.getDouble(latitudeIndex),
                        cursor.isNull(longitudeIndex) ? null : cursor.getDouble(longitudeIndex)));
            }
        }
        return movimientos;
    }

    public boolean deleteMovimiento(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("movimientos", new String[]{"cuenta_id", "tipo", "monto"},
                "id=? AND compra_id IS NULL", new String[]{String.valueOf(id)}, null, null, null)) {
            if (!cursor.moveToFirst()) return false;
            long cuentaId = cursor.getLong(0);
            String tipo = cursor.getString(1);
            double monto = cursor.getDouble(2);
            if (db.delete("movimientos", "id=?", new String[]{String.valueOf(id)}) == 0) return false;
            adjustBalance(db, cuentaId, TIPO_INGRESO.equals(tipo) ? -monto : monto);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public long insertCompra(String descripcion, String categoria, long fecha,
                             boolean programada, Long cuentaId,
                             List<ProductoCompra> productos, List<Long> participantes) {
        double total = 0;
        for (ProductoCompra producto : productos) total += producto.getSubtotal();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues purchaseValues = new ContentValues();
            purchaseValues.put("descripcion", descripcion);
            purchaseValues.put("fecha", fecha);
            purchaseValues.put("programada", programada ? 1 : 0);
            purchaseValues.put("categoria", categoria);
            if (cuentaId == null) purchaseValues.putNull("cuenta_id");
            else purchaseValues.put("cuenta_id", cuentaId);
            purchaseValues.put("total", total);
            long compraId = db.insert("compras", null, purchaseValues);
            if (compraId == -1) return -1;

            for (ProductoCompra producto : productos) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("compra_id", compraId);
                itemValues.put("nombre", producto.getNombre());
                itemValues.put("cantidad", producto.getCantidad());
                itemValues.put("precio_unitario", producto.getPrecioUnitario());
                if (db.insert("productos_compra", null, itemValues) == -1) return -1;
            }

            if (!participantes.isEmpty()) {
                double share = total / participantes.size();
                for (long contactoId : participantes) {
                    ContentValues debtValues = new ContentValues();
                    debtValues.put("compra_id", compraId);
                    debtValues.put("contacto_id", contactoId);
                    debtValues.put("monto", share);
                    debtValues.put("pagada", 0);
                    if (db.insert("deudas", null, debtValues) == -1) return -1;
                }
            }

            if (!programada && cuentaId != null) {
                if (insertMovimientoInternal(db, cuentaId, TIPO_EGRESO, total,
                        "Compra: " + descripcion, fecha, null, null, compraId) == -1) return -1;
                adjustBalance(db, cuentaId, -total);
            }

            db.setTransactionSuccessful();
            return compraId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Compra> getCompras() { return queryCompras(null); }

    public List<Compra> getComprasRecientes(int limit) { return queryCompras(String.valueOf(limit)); }

    private List<Compra> queryCompras(String limit) {
        List<Compra> compras = new ArrayList<>();
        String sql = "SELECT cp.id, cp.descripcion, cp.categoria, cp.fecha, cp.programada, cp.total, " +
                "c.nombre AS cuenta_nombre, " +
                "(SELECT COUNT(*) FROM productos_compra pc WHERE pc.compra_id=cp.id) AS items, " +
                "(SELECT COUNT(*) FROM deudas d WHERE d.compra_id=cp.id) AS participantes " +
                "FROM compras cp LEFT JOIN cuentas c ON c.id=cp.cuenta_id " +
                "ORDER BY cp.fecha DESC, cp.id DESC" + (limit == null ? "" : " LIMIT " + limit);
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                compras.add(new Compra(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getInt(4) == 1, cursor.getDouble(5),
                        cursor.getString(6), cursor.getInt(7), cursor.getInt(8)));
            }
        }
        return compras;
    }

    public Map<String, Double> getGastosPorCategoria() {
        Map<String, Double> totals = new LinkedHashMap<>();
        String sql = "SELECT categoria, COALESCE(SUM(total), 0) " +
                "FROM compras WHERE programada=0 GROUP BY categoria";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                totals.put(cursor.getString(0), cursor.getDouble(1));
            }
        }
        return totals;
    }

    public long insertPuntoMapa(String nombre, double latitud, double longitud) {
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("latitud", latitud);
        values.put("longitud", longitud);
        return getWritableDatabase().insert("puntos_mapa", null, values);
    }

    public List<PuntoMapa> getPuntosMapa() {
        List<PuntoMapa> points = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "puntos_mapa", null, null, null, null, null, "nombre COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                points.add(new PuntoMapa(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"))));
            }
        }
        return points;
    }

    public boolean deletePuntoMapa(long id) {
        return getWritableDatabase().delete(
                "puntos_mapa", "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteCompra(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery(
                "SELECT cuenta_id, total, programada FROM compras WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (!cursor.moveToFirst()) return false;
            Long cuentaId = cursor.isNull(0) ? null : cursor.getLong(0);
            double total = cursor.getDouble(1);
            boolean programada = cursor.getInt(2) == 1;
            db.delete("movimientos", "compra_id=?", new String[]{String.valueOf(id)});
            if (db.delete("compras", "id=?", new String[]{String.valueOf(id)}) == 0) return false;
            if (!programada && cuentaId != null) adjustBalance(db, cuentaId, total);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Deuda> getDeudas() {
        List<Deuda> deudas = new ArrayList<>();
        String sql = "SELECT d.id, ct.nombre, cp.descripcion, d.monto, d.pagada " +
                "FROM deudas d JOIN contactos ct ON ct.id=d.contacto_id " +
                "JOIN compras cp ON cp.id=d.compra_id ORDER BY d.pagada, cp.fecha DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                deudas.add(new Deuda(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getDouble(3), cursor.getInt(4) == 1));
            }
        }
        return deudas;
    }

    public boolean setDeudaPagada(long id, boolean pagada) {
        ContentValues values = new ContentValues();
        values.put("pagada", pagada ? 1 : 0);
        return getWritableDatabase().update(
                "deudas", values, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public double getSaldoTotal() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(saldo),0) FROM cuentas", null)) {
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        }
    }

    public double getTotalEgresos() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT " +
                        "COALESCE((SELECT SUM(monto) FROM movimientos WHERE tipo=? AND compra_id IS NULL),0) + " +
                        "COALESCE((SELECT SUM(total) FROM compras WHERE programada=0),0)",
                new String[]{TIPO_EGRESO})) {
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        }
    }

    public boolean insertProduct(Producto producto) {
        ContentValues values = new ContentValues();
        values.put("nombre", producto.getNombre());
        values.put("cantidad", producto.getCantidad());
        values.put("precio", producto.getPrecio());
        return getWritableDatabase().insert("productos", null, values) != -1;
    }
}
