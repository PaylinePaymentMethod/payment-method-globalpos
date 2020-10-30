package com.payline.payment.globalpos.utils.form;

import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.utils.constant.FormConfigurationKeys;
import com.payline.payment.globalpos.utils.i18n.I18nService;
import com.payline.pmapi.bean.paymentform.bean.field.*;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FormUtils {
    private I18nService i18n = I18nService.getInstance();


    /**
     * Holder
     */
    private static class SingletonHolder {
        /**
         * Unique instance, not preinitializes
         */
        private static final FormUtils instance = new FormUtils();
    }

    /**
     * Unique access point for the singleton instance
     */
    public static FormUtils getInstance() {
        return FormUtils.SingletonHolder.instance;
    }


        public PaymentFormConfigurationResponseSpecific createRetryForm(Locale locale, String amount, String currencySymbol) {
        if (locale == null) {
            throw new InvalidDataException("locale must not be null when creating the RETRY payment ticket form");
        }

        if (amount == null || currencySymbol == null) {
            throw new InvalidDataException("amount must not be null when creating the RETRY payment ticket form");
        }
        List<PaymentFormField> listForm = new ArrayList<>();

        // create a field text to display why the buyer has to retry
        PaymentFormDisplayFieldText displayRetryMessage = PaymentFormDisplayFieldText.PaymentFormDisplayFieldTextBuilder.aPaymentFormDisplayFieldText()
                .withContent(i18n.getMessage("formCabTitre.retryMessage", locale, amount, currencySymbol))
                .build();
        listForm.add(displayRetryMessage);

        listForm.add(createPaymentTicketInputFieldText(locale));

        CustomForm customForm = CustomForm.builder()
                .withCustomFields(listForm)
                .withDescription(i18n.getMessage("customFormTitre.description", locale))
                .build();

        return PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                .aPaymentFormConfigurationResponseSpecific()
                .withPaymentForm(customForm)
                .build();
    }


    public PaymentFormConfigurationResponseSpecific createSimpleForm(Locale locale) {
        if (locale == null) {
            throw new InvalidDataException("locale must not be null when creating the payment ticket form");
        }
        List<PaymentFormField> listForm = new ArrayList<>();
        listForm.add(createPaymentTicketInputFieldText(locale));

        CustomForm customForm = CustomForm.builder()
                .withDisplayButton(true)
                .withButtonText(i18n.getMessage("customFormTitre.buttonText", locale))
                .withCustomFields(listForm)
                .withDescription(i18n.getMessage("customFormTitre.description", locale))
                .build();

        return PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                .aPaymentFormConfigurationResponseSpecific()
                .withPaymentForm(customForm)
                .build();
    }

    /**
     * create the inputFieldText used to enter the payment ticket
     * @param locale used to create message in the right language
     * @return the inputFieldText to put in a CustomForm
     */
    public PaymentFormInputFieldText createPaymentTicketInputFieldText(Locale locale) {
        // Pattern: regexp for exactly 44 numbers
        return PaymentFormInputFieldText.PaymentFormFieldTextBuilder.aPaymentFormFieldText()
                .withRequiredErrorMessage(i18n.getMessage("formCabTitre.requiredErrorMessage", locale))
                .withValidation(Pattern.compile("^( *)[0-9]{44}( *)$"))
                .withValidationErrorMessage(i18n.getMessage("formCabTitre.validationErrorMessage", locale))
                .withPlaceholder(i18n.getMessage("formCabTitre.placeHolder", locale))
                .withKey(FormConfigurationKeys.CABTITRE)
                .withLabel(i18n.getMessage("formCabTitre.label", locale))
                .withRequired(true)
                .withInputType(InputType.NUMBER)
                .withFieldIcon(FieldIcon.CARD)
                .build();
    }
}
