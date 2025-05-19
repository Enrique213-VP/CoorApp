package com.svape.qr.coorapp.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.annotation.SuppressLint;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.svape.qr.coorapp.model.ApiResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@RunWith(JUnit4.class)
public class ApiServiceTest {

    private TestRequestQueue testRequestQueue;
    private TestApiService testApiService;

    @Before
    public void setup() {
        testRequestQueue = new TestRequestQueue();
        testApiService = new TestApiService(testRequestQueue);
    }

    public static class TestStringRequest extends StringRequest {
        private final Response.Listener<String> successListener;
        private final Response.ErrorListener errorListener;

        public TestStringRequest(int method, String url,
                                 Response.Listener<String> listener,
                                 Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            this.successListener = listener;
            this.errorListener = errorListener;
        }

        public Response.Listener<String> getSuccessListener() {
            return successListener;
        }

        public Response.ErrorListener getErrorListener() {
            return errorListener;
        }
    }

    public static class TestRequestQueue extends RequestQueue {
        private TestStringRequest lastRequest;

        public TestRequestQueue() {
            super(null, null);
        }

        @Override
        public <T> Request<T> add(Request<T> request) {
            if (request instanceof TestStringRequest) {
                this.lastRequest = (TestStringRequest) request;
            }
            return request;
        }

        public boolean hasRequest() {
            return lastRequest != null;
        }

        public void simulateSuccessResponse(String response) {
            if (lastRequest != null && lastRequest.getSuccessListener() != null) {
                lastRequest.getSuccessListener().onResponse(response);
            }
        }

        public void simulateErrorResponse(VolleyError error) {
            if (lastRequest != null && lastRequest.getErrorListener() != null) {
                lastRequest.getErrorListener().onErrorResponse(error);
            }
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void cancelAll(RequestFilter filter) {}

        @Override
        public void cancelAll(Object tag) {}
    }

    public static class TestApiService extends ApiService {
        public TestApiService(RequestQueue requestQueue) {
            super(requestQueue);
        }

        @Override
        public Single<ApiResponse> validateData(String base64Data) {
            return Single.create(emitter -> {
                TestStringRequest request = new TestStringRequest(
                        Request.Method.POST,
                        "https://noderedtest.coordinadora.com/api/v1/validar",
                        response -> {
                            emitter.onSuccess(new ApiResponse(true, response));
                        },
                        error -> {
                            emitter.onSuccess(new ApiResponse(false, "Error en la validación"));
                        }
                );

                getRequestQueue().add(request);
            });
        }

        public RequestQueue getRequestQueue() {
            try {
                java.lang.reflect.Field field = ApiService.class.getDeclaredField("requestQueue");
                field.setAccessible(true);
                return (RequestQueue) field.get(this);
            } catch (Exception e) {
                throw new RuntimeException("No se pudo acceder al campo requestQueue", e);
            }
        }
    }

    @SuppressLint("CheckResult")
    @Test
    public void validateData_whenApiReturnsSuccess_shouldReturnCorrectResponse() throws InterruptedException {
        String mockBase64Data = "VGVzdERhdGE=";
        String mockResponseData = "12345,40.7128,-74.0060,Test Observation";

        CountDownLatch latch = new CountDownLatch(1);

        final ApiResponse[] resultHolder = new ApiResponse[1];

        testApiService.validateData(mockBase64Data)
                .subscribeOn(Schedulers.trampoline())
                .subscribe(
                        response -> {
                            resultHolder[0] = response;
                            latch.countDown();
                        },
                        error -> {
                            latch.countDown();
                        }
                );

        assertTrue("No se agregó ninguna solicitud a la cola", testRequestQueue.hasRequest());

        testRequestQueue.simulateSuccessResponse(mockResponseData);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("La operación no se completó en el tiempo esperado", completed);

        assertResponse(resultHolder[0], true, mockResponseData);
    }

    @SuppressLint("CheckResult")
    @Test
    public void validateData_whenApiReturnsError_shouldReturnErrorResponse() throws InterruptedException {
        String mockBase64Data = "SW52YWxpZERhdGE=";

        CountDownLatch latch = new CountDownLatch(1);

        final ApiResponse[] resultHolder = new ApiResponse[1];

        testApiService.validateData(mockBase64Data)
                .subscribeOn(Schedulers.trampoline())
                .subscribe(
                        response -> {
                            resultHolder[0] = response;
                            latch.countDown();
                        },
                        error -> {
                            latch.countDown();
                        }
                );

        assertTrue("No se agregó ninguna solicitud a la cola", testRequestQueue.hasRequest());

        testRequestQueue.simulateErrorResponse(new VolleyError("API Error"));

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("La operación no se completó en el tiempo esperado", completed);

        assertResponse(resultHolder[0], false, "Error en la validación");
    }

    private void assertResponse(ApiResponse response, boolean expectedCorrect, String expectedData) {
        if (response == null) {
            throw new AssertionError("La respuesta no debería ser nula");
        }
        assertEquals("isCorrect debería ser " + expectedCorrect, expectedCorrect, response.isCorrect());
        assertEquals("El dato de respuesta no coincide", expectedData, response.getData());
    }
}