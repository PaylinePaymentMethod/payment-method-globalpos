package com.payline.payment.globalpos.bean.response;

import com.google.gson.GsonBuilder;
import lombok.Getter;

public class GetAuthToken extends JsonBeanResponse {
    @Getter
    String token;

    public static GetAuthToken fromJson(String json) {
        return new GsonBuilder().create().fromJson(json, GetAuthToken.class);
    }
}
