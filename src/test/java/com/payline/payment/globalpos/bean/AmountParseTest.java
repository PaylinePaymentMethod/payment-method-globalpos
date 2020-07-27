package com.payline.payment.globalpos.bean;

import com.payline.pmapi.bean.common.Amount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Currency;
import java.util.Locale;

class AmountParseTest {

    @Test
    void splitDecimal() {
        Amount amount = new Amount(new BigInteger("1000"), Currency.getInstance(Locale.FRANCE));
        Assertions.assertEquals("10.00", AmountParse.splitDecimal(amount).toString());

        amount = new Amount(new BigInteger("1050"), Currency.getInstance(Locale.FRANCE));
        Assertions.assertEquals("10.50", AmountParse.splitDecimal(amount).toString());
    }

    @Test
    void createAmount(){
        Assertions.assertEquals("100",AmountParse.createBigInteger("1", Currency.getInstance(Locale.FRANCE)).toString());
        Assertions.assertEquals("100",AmountParse.createBigInteger("1.00", Currency.getInstance(Locale.FRANCE)).toString());
        Assertions.assertEquals("1",AmountParse.createBigInteger("0.01", Currency.getInstance(Locale.FRANCE)).toString());
        Assertions.assertEquals("10",AmountParse.createBigInteger("0.1", Currency.getInstance(Locale.FRANCE)).toString());
    }
}