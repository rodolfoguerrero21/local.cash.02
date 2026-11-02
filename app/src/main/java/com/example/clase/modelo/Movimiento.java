package com.example.clase.modelo;

public class Movimiento {
    private final long id;
    private final String cuenta;
    private final String tipo;
    private final double monto;
    private final String descripcion;
    private final long fecha;
    private final Double latitud;
    private final Double longitud;

    public Movimiento(long id, String cuenta, String tipo, double monto, String descripcion,
                      long fecha, Double latitud, Double longitud) {
        this.id = id;
        this.cuenta = cuenta;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public long getId() { return id; }
    public String getCuenta() { return cuenta; }
    public String getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public long getFecha() { return fecha; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
}
