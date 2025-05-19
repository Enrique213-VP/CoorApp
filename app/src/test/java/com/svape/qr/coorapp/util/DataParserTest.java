package com.svape.qr.coorapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.svape.qr.coorapp.model.BackupItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class DataParserTest {

    @Test
    public void parseData_withValidInput_returnsBackupItem() {
        // etiqueta1d:ABC123-latitud:10.12345-longitud:-80.67890-observacion:Esta es una observación

        String validInput = "etiqueta1d:ABC123-latitud:10.12345-longitud:-80.67890-observacion:Esta es una observación de prueba";

        BackupItem result = DataParser.parseData(validInput);

        assertNotNull(result);
        assertEquals("ABC123", result.getEtiqueta1d());
        assertEquals(10.12345, result.getLatitud(), 0.00001);
        assertEquals(-80.67890, result.getLongitud(), 0.00001);
        assertEquals("Esta es una observación de prueba", result.getObservacion());
    }

    @Test
    public void parseData_withEmptyInput_returnsEmptyBackupItem() {
        String emptyInput = "";

        BackupItem result = DataParser.parseData(emptyInput);

        assertNotNull("Debería devolver un objeto BackupItem vacío, no null", result);
    }

    @Test
    public void formatInput_withValidInput_returnsFormattedString() {
        // etiqueta-latitud-longitud-observacion
        String rawInput = "ABC123-10.12345--80.67890-Observación con guiones";
        String expected = "etiqueta1d:ABC123-latitud:10.12345-longitud:-80.6789-observacion:Observación con guiones";

        String result = DataParser.formatInput(rawInput);

        assertEquals(expected, result);
    }

    @Test
    public void formatInput_withInvalidLatitude_throwsException() {
        // Latitud no numérica
        String invalidInput = "ABC123-texto-no-numerico--80.67890-Observación";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            DataParser.formatInput(invalidInput);
        });

        assertEquals("Latitud inválida: debe ser un número decimal: 'texto'", exception.getMessage());
    }

    @Test
    public void formatInput_withLatitudeOutOfRange_throwsException() {
        // Latitud fuera de rango (-90 a 90)
        String invalidInput = "ABC123-95.0--80.67890-Observación";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            DataParser.formatInput(invalidInput);
        });

        assertEquals("Latitud fuera de rango (-90 a 90): 95.0", exception.getMessage());
    }


    @Test
    public void formatInput_withLongitudeOutOfRange_throwsException() {
        // Longitud fuera de rango (-180 a 180)
        String invalidInput = "ABC123-45.0-190.0-Observación";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            DataParser.formatInput(invalidInput);
        });

        assertEquals("Longitud fuera de rango (-180 a 180): 190.0", exception.getMessage());
    }

}