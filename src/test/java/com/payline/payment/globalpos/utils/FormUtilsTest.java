package com.payline.payment.globalpos.utils;


import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.utils.form.FormUtils;
import com.payline.payment.globalpos.utils.i18n.I18nService;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FormUtilsTest {

    private FormUtils formUtils = FormUtils.getInstance();
    private I18nService i18n = I18nService.getInstance();

    private static final String AMOUNT = "12";
    private static final String CURRENCY_SYMBOL = "€";
    private static final Locale LOCALE = Locale.FRANCE;

    @Test
    void createRetryForm() {

        PaymentFormConfigurationResponseSpecific paymentFormConfigurationResponseSpecific = formUtils.createRetryForm(LOCALE, AMOUNT, CURRENCY_SYMBOL);

        Assertions.assertEquals(paymentFormConfigurationResponseSpecific.getPaymentForm().getDescription(), i18n.getMessage("customFormTitre.description", LOCALE));
    }

    @Test
    void createRetryFormWithNullAmount() {
        assertThrows(InvalidDataException.class, () -> formUtils.createRetryForm(LOCALE, null, CURRENCY_SYMBOL));
    }

    @Test
    void createRetryFormWithNullCurrencySymbol() {
        assertThrows(InvalidDataException.class, () -> formUtils.createRetryForm(LOCALE, AMOUNT, null));
    }

    @Test
    void createRetryFormWithNullLocale() {
        assertThrows(InvalidDataException.class, () -> formUtils.createRetryForm(null, AMOUNT, CURRENCY_SYMBOL));
    }
}
