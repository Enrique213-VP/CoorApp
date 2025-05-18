package com.svape.qr.coorapp.model;

public class ApiResponse {
    private boolean isCorrect;
    private String data;

    public ApiResponse(boolean isCorrect, String data) {
        this.isCorrect = isCorrect;
        this.data = data;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public String getData() {
        return data;
    }
}