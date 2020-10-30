package com.payline.payment.globalpos.bean.response;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.exception.InvalidDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTitreDetailTransacTest {

    @Test
    void fromXml() {
        GetTitreDetailTransac value = GetTitreDetailTransac.fromXml(MockUtils.getTitreTransacOK());
        Assertions.assertNotNull(value.getCodeErreur());
        Assertions.assertNotNull(value.getTitre());
        Assertions.assertNotNull(value.getEmetteur());
        Assertions.assertNotNull(value.getMontant());
        Assertions.assertNotNull(value.getDateValid());
        Assertions.assertNotNull(value.getNumTitre());
        Assertions.assertNotNull(value.getId());
        Assertions.assertEquals("1", value.getCodeErreur());
        Assertions.assertEquals("INCO", value.getTitre());
        Assertions.assertEquals("940001", value.getEmetteur());
        Assertions.assertEquals("10", value.getMontant());
        Assertions.assertEquals("12/10/2020", value.getDateValid());
        Assertions.assertEquals("3135057060", value.getNumTitre());
        Assertions.assertEquals("5e81e6db55962", value.getId());
    }

    @Test
    void noXml() {

        String s = MockUtils.noXml();
        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> GetTitreDetailTransac.fromXml(s));


        Assertions.assertEquals("Unable to parse XML GetTitreDetailTransac", thrown.getMessage());
    }
}