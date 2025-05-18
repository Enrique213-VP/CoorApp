package com.svape.qr.coorapp.repository.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "backup_local")
public class BackupItemEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String etiqueta1d;
    private double latitud;
    private double longitud;
    private String observacion;

    public BackupItemEntity(String etiqueta1d, double latitud, double longitud, String observacion) {
        this.etiqueta1d = etiqueta1d;
        this.latitud = latitud;
        this.longitud = longitud;
        this.observacion = observacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEtiqueta1d() {
        return etiqueta1d;
    }

    public void setEtiqueta1d(String etiqueta1d) {
        this.etiqueta1d = etiqueta1d;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}