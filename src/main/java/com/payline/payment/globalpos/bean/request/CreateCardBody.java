package com.payline.payment.globalpos.bean.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Class used to create the body of the setCreateCard API call
 */
@Builder
public class CreateCardBody {

    @Getter
    @NonNull
    private String dateTransac;

    @Getter
    @NonNull
    private String typeTitre;

    @Getter
    @NonNull
    private String magCaisse;

    @Getter
    @NonNull
    private String email;

    @Getter
    private String action;

    @Getter
    private int montant;

    @Getter
    private String numTransac;

    public String toJson() {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        return gson.toJson(this);
    }

    /**
     * Enum containing all possible values for action field
     */
    public enum Action {
        CREATION
    }

    /**
     * Enum containing all possible values for typeTitre field
     */
    public enum Title {
        TITLE940001("940001");

        private String name;

        Title(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
