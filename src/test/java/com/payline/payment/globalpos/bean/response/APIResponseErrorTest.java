package com.payline.payment.globalpos.bean.response;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.exception.InvalidDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class APIResponseErrorTest {

    @Test
    void fromXml() {
        APIResponseError value = APIResponseError.fromXml(MockUtils.getTransacKO());
        Assertions.assertNotNull(value);
        Assertions.assertNotNull(value.getHttpStatus());
        Assertions.assertNotNull(value.getMessage());
        Assertions.assertEquals("200", value.getHttpStatus());
        Assertions.assertEquals(-50, value.getError());
        Assertions.assertEquals("Magasin inconnu de l'enseigne", value.getMessage());
    }

    @Test
    void noXml() {

        String s = MockUtils.noXml();
        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> APIResponseError.fromXml(s));

        Assertions.assertEquals("Unable to parse XML GlobalPOSAPIResponse", thrown.getMessage());
    }
}