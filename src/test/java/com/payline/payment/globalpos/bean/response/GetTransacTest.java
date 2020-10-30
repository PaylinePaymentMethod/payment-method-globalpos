package com.payline.payment.globalpos.bean.response;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.exception.InvalidDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTransacTest {

    @Test
    void fromXml() {
        GetTransac value = GetTransac.fromXml(MockUtils.getTransacOK());
        Assertions.assertNotNull(value.getCodeErreur());
        Assertions.assertNotNull(value.getNumTransac());
        Assertions.assertEquals("1", value.getCodeErreur());
        Assertions.assertEquals("5e7db72846ebd", value.getNumTransac());
    }

    @Test
    void noXml() {

        String s = MockUtils.noXml();
        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> GetTransac.fromXml(s));

        Assertions.assertEquals("Unable to parse XML GetTransac", thrown.getMessage());
    }
}