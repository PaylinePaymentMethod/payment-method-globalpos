package com.payline.payment.globalpos.bean.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Class used to create the body of the getAuthToken API call
 */
@AllArgsConstructor
public class LoginBody {

    @Getter
    @NonNull
    private String login;

    @Getter
    @NonNull
    private String password;

    @Getter
    @NonNull
    private String guid;

    public String toJson() {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        return gson.toJson(this);
    }


}
