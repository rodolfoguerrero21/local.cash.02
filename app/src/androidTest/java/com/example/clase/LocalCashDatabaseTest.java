package com.example.clase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Compra;
import com.example.clase.modelo.Deuda;
import com.example.clase.modelo.Movimiento;
import com.example.clase.modelo.ProductoCompra;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LocalCashDatabaseTest {

    private static final String TEST_DATABASE = "localcash-test.db";

    private Context testContext;
    private DatabaseHelper database;

    @Before
    public void setUp() {
        testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testContext.deleteDatabase(TEST_DATABASE);
        database = new DatabaseHelper(testContext, TEST_DATABASE);
    }

    @After
    public void tearDown() {
        database.close();
        testContext.deleteDatabase(TEST_DATABASE);
    }

    @Test
    public void completeFinancialFlowPersistsAndBalancesData() {
        long accountId = database.insertCuenta("Efectivo", "Efectivo", 100000);
        long contactId = database.insertContacto("Ana", "123456", "ana@local.cl");

        assertTrue(accountId > 0);
        assertTrue(contactId > 0);
        assertTrue(database.insertMovimiento(accountId, DatabaseHelper.TIPO_EGRESO,
                10000, "Transporte", System.currentTimeMillis(), -33.45, -70.66) > 0);
        assertEquals(90000, database.getSaldoTotal(), 0.01);

        Map<Long, Double> participantContributions = new LinkedHashMap<>();
        participantContributions.put(contactId, 4000.0);
        long purchaseId = database.insertCompra(
                "Compra compartida",
                "Alimentación",
                System.currentTimeMillis(),
                false,
                accountId,
                Arrays.asList(
                        new ProductoCompra("Pan", 2, 2500),
                        new ProductoCompra("Leche", 1, 5000)),
                participantContributions);

        assertTrue(purchaseId > 0);
        assertEquals(84000, database.getSaldoTotal(), 0.01);
        assertEquals(16000, database.getTotalEgresos(), 0.01);
        Map<String, Double> categoryTotals = database.getGastosPorCategoria();
        assertEquals(6000, categoryTotals.get("Alimentación"), 0.01);
        assertEquals(2, database.getCompras().get(0).getCantidadProductos());
        assertEquals(6000, database.getCompras().get(0).getMontoCuenta(), 0.01);
        assertEquals(1, database.getDeudas().size());
        assertEquals(4000, database.getDeudas().get(0).getMonto(), 0.01);

        Deuda debt = database.getDeudas().get(0);
        assertTrue(database.setDeudaPagada(debt.getId(), true));
        assertTrue(database.getDeudas().get(0).isPagada());

        long futurePurchaseId = database.insertCompra(
                "Compra futura",
                "Estudios",
                System.currentTimeMillis() + 86400000L,
                true,
                accountId,
                Collections.singletonList(new ProductoCompra("Materiales", 1, 20000)),
                Collections.emptyMap());
        assertTrue(futurePurchaseId > 0);
        assertEquals(64000, database.getSaldoTotal(), 0.01);
        assertEquals(36000, database.getTotalEgresos(), 0.01);
        assertEquals(2, database.getCompras().size());
        assertEquals(20000, database.getGastosPorCategoria().get("Estudios"), 0.01);

        assertTrue(database.deleteCompra(purchaseId));
        assertEquals(70000, database.getSaldoTotal(), 0.01);
        assertTrue(database.getDeudas().isEmpty());
        assertTrue(database.deleteCompra(futurePurchaseId));
        assertEquals(90000, database.getSaldoTotal(), 0.01);
        assertFalse(database.getMovimientos().isEmpty());

        long pointId = database.insertPuntoMapa("Instituto", -33.45, -70.66);
        assertTrue(pointId > 0);
        assertEquals("Instituto", database.getPuntosMapa().get(0).getNombre());
        assertTrue(database.deletePuntoMapa(pointId));
        assertTrue(database.getPuntosMapa().isEmpty());
    }

    @Test
    public void participantContributionsCannotExceedPurchaseTotal() {
        long accountId = database.insertCuenta("Efectivo", "Efectivo", 50000);
        long contactId = database.insertContacto("Luis", "987654321", "luis@local.cl");
        Map<Long, Double> contributions = new LinkedHashMap<>();
        contributions.put(contactId, 15000.0);

        long purchaseId = database.insertCompra(
                "Compra inválida",
                "Alimentación",
                System.currentTimeMillis(),
                false,
                accountId,
                Collections.singletonList(new ProductoCompra("Producto", 1, 10000)),
                contributions);

        assertEquals(-1, purchaseId);
        assertEquals(50000, database.getSaldoTotal(), 0.01);
        assertTrue(database.getCompras().isEmpty());
        assertTrue(database.getDeudas().isEmpty());
    }

    @Test
    public void futurePurchaseCanBeEditedAndRecalculatesAccountBalance() {
        long accountId = database.insertCuenta("Cuenta principal", "Efectivo", 100000);
        long contactId = database.insertContacto("Camila", "987654321", "camila@local.cl");
        long purchaseId = database.insertCompra(
                "Compra futura original",
                "Estudios",
                System.currentTimeMillis() + 86400000L,
                true,
                accountId,
                Collections.singletonList(new ProductoCompra("Cuaderno", 1, 20000)),
                Collections.emptyMap());

        assertTrue(purchaseId > 0);
        assertEquals(80000, database.getSaldoTotal(), 0.01);
        assertEquals("Cuaderno", database.getProductosCompra(purchaseId).get(0).getNombre());

        Map<Long, Double> contributions = new LinkedHashMap<>();
        contributions.put(contactId, 5000.0);
        assertTrue(database.updateCompra(
                purchaseId,
                "Compra futura editada",
                "Alimentación",
                System.currentTimeMillis() + 172800000L,
                true,
                accountId,
                Collections.singletonList(new ProductoCompra("Mercadería", 2, 15000)),
                contributions));

        Compra updatedPurchase = database.getCompra(purchaseId);
        assertNotNull(updatedPurchase);
        assertEquals("Compra futura editada", updatedPurchase.getDescripcion());
        assertTrue(updatedPurchase.isProgramada());
        assertEquals(30000, updatedPurchase.getTotal(), 0.01);
        assertEquals(25000, updatedPurchase.getMontoCuenta(), 0.01);
        assertEquals(75000, database.getSaldoTotal(), 0.01);
        assertEquals(1, database.getMovimientos().size());
        assertEquals(2, database.getProductosCompra(purchaseId).get(0).getCantidad());
        assertEquals(5000, database.getAportesCompra(purchaseId).get(contactId), 0.01);

        assertTrue(database.deleteCompra(purchaseId));
        assertEquals(100000, database.getSaldoTotal(), 0.01);
    }

    @Test
    public void purchaseMapLocationIsStoredInPurchaseMovement() {
        long accountId = database.insertCuenta("Efectivo", "Efectivo", 50000);
        long purchaseId = database.insertCompra(
                "Salida al cine",
                "Gastos Personales / Estilo de Vida",
                System.currentTimeMillis(),
                false,
                accountId,
                Collections.singletonList(new ProductoCompra("Entrada", 1, 12000)),
                Collections.emptyMap(),
                "Cine del centro",
                -33.4489,
                -70.6693);

        assertTrue(purchaseId > 0);
        Compra purchase = database.getCompra(purchaseId);
        assertNotNull(purchase);
        assertEquals("Cine del centro", purchase.getUbicacionNombre());
        assertEquals(-33.4489, purchase.getLatitud(), 0.0001);
        assertEquals(-70.6693, purchase.getLongitud(), 0.0001);

        Movimiento movement = database.getMovimientos().get(0);
        assertTrue(movement.getDescripcion().contains("Lugar: Cine del centro"));
        assertEquals(-33.4489, movement.getLatitud(), 0.0001);
        assertEquals(-70.6693, movement.getLongitud(), 0.0001);
    }
}
