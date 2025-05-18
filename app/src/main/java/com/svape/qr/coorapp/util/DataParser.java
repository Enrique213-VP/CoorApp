package com.svape.qr.coorapp.util;

import android.util.Log;
import com.svape.qr.coorapp.model.BackupItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataParser {

    public static BackupItem parseData(String data) {
        Log.d("DataParser", "Parseando data: " + data);

        BackupItem item = new BackupItem();

        try {
            Pattern etiquetaPattern = Pattern.compile("etiqueta1d:([^-]+)-");
            Pattern latitudPattern = Pattern.compile("latitud:([-0-9.]+)-");
            Pattern longitudPattern = Pattern.compile("longitud:([-0-9.]+)-");
            Pattern observacionPattern = Pattern.compile("observacion:(.+)$");

            Matcher etiquetaMatcher = etiquetaPattern.matcher(data);
            if (etiquetaMatcher.find()) {
                String etiqueta = etiquetaMatcher.group(1);
                item.setEtiqueta1d(etiqueta);
                Log.d("DataParser", "Etiqueta extraída: " + etiqueta);
            }

            Matcher latitudMatcher = latitudPattern.matcher(data);
            if (latitudMatcher.find()) {
                try {
                    String latitudStr = latitudMatcher.group(1);
                    Log.d("DataParser", "Latitud string: " + latitudStr);
                    double latitud = Double.parseDouble(latitudStr);
                    item.setLatitud(latitud);
                    Log.d("DataParser", "Latitud extraída: " + latitud);
                } catch (NumberFormatException e) {
                    Log.e("DataParser", "Error al parsear latitud: " + e.getMessage(), e);
                }
            }

            Matcher longitudMatcher = longitudPattern.matcher(data);
            if (longitudMatcher.find()) {
                try {
                    String longitudStr = longitudMatcher.group(1);
                    Log.d("DataParser", "Longitud string: " + longitudStr);
                    double longitud = Double.parseDouble(longitudStr);
                    item.setLongitud(longitud);
                    Log.d("DataParser", "Longitud extraída: " + longitud);
                } catch (NumberFormatException e) {
                    Log.e("DataParser", "Error al parsear longitud: " + e.getMessage(), e);
                }
            }

            Matcher observacionMatcher = observacionPattern.matcher(data);
            if (observacionMatcher.find()) {
                String observacion = observacionMatcher.group(1);
                item.setObservacion(observacion);
                Log.d("DataParser", "Observación extraída: " + observacion);
            }
        } catch (Exception e) {
            Log.e("DataParser", "Error al parsear datos: " + e.getMessage(), e);
        }

        return item;
    }

    public static String formatInput(String input) {
        // etiqueta-latitud-longitud-observacion
        String[] parts = input.split("-");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato inválido");
        }

        String formattedInput = "etiqueta1d:" + parts[0] +
                "-latitud:" + parts[1] +
                "-longitud:" + parts[2] +
                "-observacion:" + parts[3];

        return formattedInput;
    }
}