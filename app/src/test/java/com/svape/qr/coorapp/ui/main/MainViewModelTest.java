package com.svape.qr.coorapp.ui.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.svape.qr.coorapp.util.DataParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@RunWith(BlockJUnit4ClassRunner.class)
public class MainViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testDataParser_formatInput() {
        // etiqueta-latitud-longitud-observacion

        // Formato básico con todos los campos
        String input1 = "ABC123-40.7128-74.006-Observación de prueba";
        String result1 = DataParser.formatInput(input1);
        assertEquals("etiqueta1d:ABC123-latitud:40.7128-longitud:74.006-observacion:Observación de prueba",
                result1);

        // Latitud negativa
        String input2 = "DEF456--35.6895-120.4243-Santiago observación";
        String result2 = DataParser.formatInput(input2);
        assertEquals("etiqueta1d:DEF456-latitud:-35.6895-longitud:120.4243-observacion:Santiago observación",
                result2);

        // Longitud negativa
        String input3 = "GHI789-51.5074--0.1278-Londres observación";
        String result3 = DataParser.formatInput(input3);
        assertEquals("etiqueta1d:GHI789-latitud:51.5074-longitud:-0.1278-observacion:Londres observación",
                result3);

        // Sin observación
        String input4 = "JKL012-37.7749--122.4194-";
        String result4 = DataParser.formatInput(input4);
        assertEquals("etiqueta1d:JKL012-latitud:37.7749-longitud:-122.4194-observacion:",
                result4);
    }

    @Test
    public void testDateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(new Date());

        assertTrue("La fecha debe tener el formato YYYY-MM-DD",
                dateString.matches("\\d{4}-\\d{2}-\\d{2}"));
    }
}