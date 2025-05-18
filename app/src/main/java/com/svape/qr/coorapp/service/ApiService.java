package com.svape.qr.coorapp.service;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.svape.qr.coorapp.model.ApiResponse;
import org.json.JSONException;
import org.json.JSONObject;
import io.reactivex.rxjava3.core.Single;

public class ApiService {
    private static final String API_ENDPOINT = "https://noderedtest.coordinadora.com/api/v1/validar";
    private final RequestQueue requestQueue;

    public ApiService(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public Single<ApiResponse> validateData(String base64Data) {
        return Single.create(emitter -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("data", base64Data);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        API_ENDPOINT,
                        jsonBody,
                        response -> {
                            try {
                                String correcto = response.getString("Correcto");
                                String data = response.getString("data");
                                boolean isCorrect = "estructura Correcta".equals(correcto);

                                ApiResponse apiResponse = new ApiResponse(isCorrect, data);
                                emitter.onSuccess(apiResponse);
                            } catch (JSONException e) {
                                emitter.onError(e);
                            }
                        },
                        error -> emitter.onError(error)
                );

                requestQueue.add(request);
            } catch (JSONException e) {
                emitter.onError(e);
            }
        });
    }
}