package com.payline.payment.globalpos.bean.response;

import com.google.gson.GsonBuilder;
import lombok.Getter;

public class JsonBeanResponse {
    @Getter
    private int error;

    @Getter
    private String message;

    public boolean isOk() {
        return (0 == this.error);
    }

    public static JsonBeanResponse fromJson(String json) {
        return new GsonBuilder().create().fromJson(json, JsonBeanResponse.class);
    }

}
