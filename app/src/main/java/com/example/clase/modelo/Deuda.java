package com.example.clase.modelo;

public class Deuda {
    private final long id;
    private final String contacto;
    private final String compra;
    private final double monto;
    private final boolean pagada;

    public Deuda(long id, String contacto, String compra, double monto, boolean pagada) {
        this.id = id;
        this.contacto = contacto;
        this.compra = compra;
        this.monto = monto;
        this.pagada = pagada;
    }

    public long getId() { return id; }
    public String getContacto() { return contacto; }
    public String getCompra() { return compra; }
    public double getMonto() { return monto; }
    public boolean isPagada() { return pagada; }
}
