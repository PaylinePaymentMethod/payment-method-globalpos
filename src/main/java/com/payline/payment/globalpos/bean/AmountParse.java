package com.payline.payment.globalpos.bean;

import com.payline.pmapi.bean.common.Amount;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;

public class AmountParse {

    // can't create an amountParse object
    private AmountParse() {
    }

    // put the coma on the right place depending of the currency of payline
    // add 0 if needed, before or after
    public static BigDecimal splitDecimal(Amount amount) {
        int nbDigits = amount.getCurrency().getDefaultFractionDigits();
        final BigDecimal bigDecimal = new BigDecimal(amount.getAmountInSmallestUnit());
        return bigDecimal.movePointLeft(nbDigits);
    }


    /**
     * Create a BigInteger amount from a String and a currency
     *
     * @param amount   the String amount to convert
     * @param currency used to know the digit number
     * @return an amount in BigInteger format
     */
    public static BigInteger createBigInteger(String amount, Currency currency) {
        int l = currency.getDefaultFractionDigits();
        String a = amount.replace(".", "");

        StringBuilder sb = new StringBuilder();
        sb.append(a);

        if (!amount.contains(".")) {
            for (int i = 0; i < l; i++) {
                sb.append("0");
            }
        } else {
            int n = amount.length() - (amount.indexOf('.') + 1);
            for (int i = n; i < l; i++) {
                sb.append("0");
            }
        }

        return new BigInteger(sb.toString());
    }
}