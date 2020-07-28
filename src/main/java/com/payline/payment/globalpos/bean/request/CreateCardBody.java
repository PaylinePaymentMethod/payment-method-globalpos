package com.payline.payment.globalpos.bean.request;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
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
    @SerializedName("datetransac")
    private String dateTransac;

    @Getter
    @NonNull
    @SerializedName("typetitre")
    private String typeTitre;

    @Getter
    @NonNull
    @SerializedName("magcaisse")
    private String magCaisse;

    @Getter
    @NonNull
    private String email;

    @Getter
    private String action;

    @Getter
    private int montant;

    @Getter
    @SerializedName("numtransac")
    private String numTransac;

    public String toJson() {
        return new GsonBuilder().create().toJson(this);
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
