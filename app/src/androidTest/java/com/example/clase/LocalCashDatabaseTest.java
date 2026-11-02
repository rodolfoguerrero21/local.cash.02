package com.example.clase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Deuda;
import com.example.clase.modelo.ProductoCompra;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
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

        long purchaseId = database.insertCompra(
                "Compra compartida",
                "Alimentación",
                System.currentTimeMillis(),
                false,
                accountId,
                Arrays.asList(
                        new ProductoCompra("Pan", 2, 2500),
                        new ProductoCompra("Leche", 1, 5000)),
                Collections.singletonList(contactId));

        assertTrue(purchaseId > 0);
        assertEquals(80000, database.getSaldoTotal(), 0.01);
        assertEquals(20000, database.getTotalEgresos(), 0.01);
        Map<String, Double> categoryTotals = database.getGastosPorCategoria();
        assertEquals(10000, categoryTotals.get("Alimentación"), 0.01);
        assertEquals(2, database.getCompras().get(0).getCantidadProductos());
        assertEquals(1, database.getDeudas().size());
        assertEquals(10000, database.getDeudas().get(0).getMonto(), 0.01);

        Deuda debt = database.getDeudas().get(0);
        assertTrue(database.setDeudaPagada(debt.getId(), true));
        assertTrue(database.getDeudas().get(0).isPagada());

        assertTrue(database.insertCompra(
                "Compra futura",
                "Estudios",
                System.currentTimeMillis() + 86400000L,
                true,
                accountId,
                Collections.singletonList(new ProductoCompra("Materiales", 1, 20000)),
                Collections.emptyList()) > 0);
        assertEquals(80000, database.getSaldoTotal(), 0.01);
        assertEquals(2, database.getCompras().size());
        assertFalse(database.getGastosPorCategoria().containsKey("Estudios"));

        assertTrue(database.deleteCompra(purchaseId));
        assertEquals(90000, database.getSaldoTotal(), 0.01);
        assertTrue(database.getDeudas().isEmpty());
        assertFalse(database.getMovimientos().isEmpty());

        long pointId = database.insertPuntoMapa("Instituto", -33.45, -70.66);
        assertTrue(pointId > 0);
        assertEquals("Instituto", database.getPuntosMapa().get(0).getNombre());
        assertTrue(database.deletePuntoMapa(pointId));
        assertTrue(database.getPuntosMapa().isEmpty());
    }
}
