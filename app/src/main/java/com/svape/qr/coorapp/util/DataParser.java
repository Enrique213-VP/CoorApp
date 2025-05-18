package com.svape.qr.coorapp.util;

import com.svape.qr.coorapp.model.BackupItem;

public class DataParser {

    public static BackupItem parseData(String data) {
        //etiqueta1d:XXX-latitud:XXX-longitud:XXX-observacion:XXX
        String[] parts = data.split("-");

        BackupItem item = new BackupItem();

        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                switch (key) {
                    case "etiqueta1d":
                        item.setEtiqueta1d(value);
                        break;
                    case "latitud":
                        item.setLatitud(Double.parseDouble(value));
                        break;
                    case "longitud":
                        item.setLongitud(Double.parseDouble(value));
                        break;
                    case "observacion":
                        item.setObservacion(value);
                        break;
                }
            }
        }

        return item;
    }

    public static String formatInput(String input) {
        // etiqueta1d:XXX-latitud:XXX-longitud:XXX-observacion:XXX

        String[] parts = input.split("-");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato inválido");
        }

        return "etiqueta1d:" + parts[0] +
                "-latitud:" + parts[1] +
                "-longitud:" + parts[2] +
                "-observacion:" + parts[3];
    }
}