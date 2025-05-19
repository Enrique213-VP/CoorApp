package com.svape.qr.coorapp.util;

import android.util.Log;
import com.svape.qr.coorapp.model.BackupItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataParser {
    private static final String TAG = "DataParser";

    public static BackupItem parseData(String data) {
        Log.d(TAG, "Parseando data: " + data);

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
                Log.d(TAG, "Etiqueta extraída: " + etiqueta);
            }

            Matcher latitudMatcher = latitudPattern.matcher(data);
            if (latitudMatcher.find()) {
                try {
                    String latitudStr = latitudMatcher.group(1);
                    Log.d(TAG, "Latitud string: " + latitudStr);
                    double latitud = Double.parseDouble(latitudStr);
                    item.setLatitud(latitud);
                    Log.d(TAG, "Latitud extraída: " + latitud);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error al parsear latitud: " + e.getMessage(), e);
                }
            }

            Matcher longitudMatcher = longitudPattern.matcher(data);
            if (longitudMatcher.find()) {
                try {
                    String longitudStr = longitudMatcher.group(1);
                    Log.d(TAG, "Longitud string: " + longitudStr);
                    double longitud = Double.parseDouble(longitudStr);
                    item.setLongitud(longitud);
                    Log.d(TAG, "Longitud extraída: " + longitud);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error al parsear longitud: " + e.getMessage(), e);
                }
            }

            Matcher observacionMatcher = observacionPattern.matcher(data);
            if (observacionMatcher.find()) {
                String observacion = observacionMatcher.group(1);
                item.setObservacion(observacion);
                Log.d(TAG, "Observación extraída: " + observacion);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al parsear datos: " + e.getMessage(), e);
        }

        return item;
    }

    public static String formatInput(String input) {
        Log.d(TAG, "Entrada original: " + input);

        String[] parts = new String[4];

        try {
            int firstDashIndex = input.indexOf('-');
            if (firstDashIndex <= 0) {
                throw new IllegalArgumentException("Formato inválido: no se encuentra etiqueta");
            }

            parts[0] = input.substring(0, firstDashIndex).trim();

            int currentIndex = firstDashIndex + 1;

            boolean isLatNegative = currentIndex < input.length() && input.charAt(currentIndex) == '-';
            if (isLatNegative) {
                currentIndex++;
            }

            int nextDashIndex = input.indexOf('-', currentIndex);
            if (nextDashIndex == -1) {
                throw new IllegalArgumentException("Formato inválido: no se encuentra separador después de la latitud");
            }

            String latitudStr = input.substring(currentIndex, nextDashIndex).trim();
            if (isLatNegative) {
                latitudStr = "-" + latitudStr;
            }
            parts[1] = latitudStr;

            currentIndex = nextDashIndex + 1;

            boolean isLongNegative = currentIndex < input.length() && input.charAt(currentIndex) == '-';
            if (isLongNegative) {
                currentIndex++;
            }

            nextDashIndex = input.indexOf('-', currentIndex);
            if (nextDashIndex == -1) {
                parts[2] = input.substring(currentIndex).trim();
                parts[3] = "";
            } else {
                String longitudStr = input.substring(currentIndex, nextDashIndex).trim();
                if (isLongNegative) {
                    longitudStr = "-" + longitudStr;
                }
                parts[2] = longitudStr;

                parts[3] = input.substring(nextDashIndex + 1).trim();
            }

            Log.d(TAG, "Componentes extraídos - Etiqueta: '" + parts[0] +
                    "', Latitud: '" + parts[1] +
                    "', Longitud: '" + parts[2] +
                    "', Observación: '" + parts[3] + "'");

            validateAndTransformParts(parts);

            String formattedInput = "etiqueta1d:" + parts[0] +
                    "-latitud:" + parts[1] +
                    "-longitud:" + parts[2] +
                    "-observacion:" + parts[3];

            Log.d(TAG, "Entrada formateada: " + formattedInput);
            return formattedInput;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Error al formatear entrada: " + e.getMessage(), e);
            throw new IllegalArgumentException("Error al procesar entrada: " + e.getMessage());
        }
    }

    private static void validateAndTransformParts(String[] parts) {
        if (parts[0] == null || parts[0].isEmpty()) {
            throw new IllegalArgumentException("La etiqueta no puede estar vacía");
        }

        try {
            double latitud = Double.parseDouble(parts[1]);
            if (latitud < -90 || latitud > 90) {
                throw new IllegalArgumentException("Latitud fuera de rango (-90 a 90): " + latitud);
            }

            parts[1] = String.valueOf(latitud);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Latitud inválida: debe ser un número decimal: '" + parts[1] + "'");
        }

        try {
            double longitud = Double.parseDouble(parts[2]);
            if (longitud < -180 || longitud > 180) {
                throw new IllegalArgumentException("Longitud fuera de rango (-180 a 180): " + longitud);
            }

            parts[2] = String.valueOf(longitud);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Longitud inválida: debe ser un número decimal: '" + parts[2] + "'");
        }

        if (parts[3] == null) {
            parts[3] = "";
        }
    }
}