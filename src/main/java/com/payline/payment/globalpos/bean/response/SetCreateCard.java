package com.payline.payment.globalpos.bean.response;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public class SetCreateCard extends JsonBeanResponse {

    @Getter
    @SerializedName("cartes")
    public Card card;

    public class Card {
        @Getter
        @SerializedName("cardid")
        String cardId;
        @Getter
        @SerializedName("cardid2")
        String cardId2;
        @Getter
        @SerializedName("cardcvv")
        String cardCvv;
        @Getter
        @SerializedName("montant")
        String amount;
    }

    public static SetCreateCard fromJson(String json) {
        return new GsonBuilder().create().fromJson(json, SetCreateCard.class);
    }
}
