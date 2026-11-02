package com.example.clase.modelo;

public class Compra {
    private final long id;
    private final String descripcion;
    private final String categoria;
    private final long fecha;
    private final boolean programada;
    private final double total;
    private final double montoCuenta;
    private final Long cuentaId;
    private final String cuenta;
    private final int cantidadProductos;
    private final int participantes;

    public Compra(long id, String descripcion, String categoria, long fecha, boolean programada,
                  double total, double montoCuenta, Long cuentaId, String cuenta,
                  int cantidadProductos, int participantes) {
        this.id = id;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.fecha = fecha;
        this.programada = programada;
        this.total = total;
        this.montoCuenta = montoCuenta;
        this.cuentaId = cuentaId;
        this.cuenta = cuenta;
        this.cantidadProductos = cantidadProductos;
        this.participantes = participantes;
    }

    public long getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public long getFecha() { return fecha; }
    public boolean isProgramada() { return programada; }
    public double getTotal() { return total; }
    public double getMontoCuenta() { return montoCuenta; }
    public Long getCuentaId() { return cuentaId; }
    public String getCuenta() { return cuenta; }
    public int getCantidadProductos() { return cantidadProductos; }
    public int getParticipantes() { return participantes; }
}
