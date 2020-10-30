package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.service.LogoPaymentFormConfigurationService;
import com.payline.payment.globalpos.utils.form.FormUtils;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;

public class PaymentFormConfigurationServiceImpl extends LogoPaymentFormConfigurationService {
    private FormUtils formUtils = FormUtils.getInstance();

    @Override
    public PaymentFormConfigurationResponse getPaymentFormConfiguration(PaymentFormConfigurationRequest request) {
        return formUtils.createSimpleForm(request.getLocale());
    }
}
