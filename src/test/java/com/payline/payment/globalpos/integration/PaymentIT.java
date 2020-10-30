package com.payline.payment.globalpos.integration;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.service.impl.ConfigurationServiceImpl;
import com.payline.payment.globalpos.service.impl.PaymentServiceImpl;
import com.payline.payment.globalpos.utils.constant.FormConfigurationKeys;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.payment.PaymentFormContext;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.service.ConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.payline.payment.globalpos.utils.constant.RequestContextKeys.*;

class PaymentIT {

    private ConfigurationService configurationService = new ConfigurationServiceImpl();
    private PaymentServiceImpl paymentService = new PaymentServiceImpl();

    PaymentRequest createPaymentRequest(RequestContext context) {

        PaymentRequest.Builder request = MockUtils.aPaylinePaymentRequestBuilder()
                .withRequestContext(MockUtils.aRequestContextBuilderStep(
                        context.getRequestData().get(CONTEXT_DATA_STEP),
                        context.getRequestData().get(NUMTRANSAC))
                        .build()
                );
        if (context.getRequestData().get(CONTEXT_DATA_STEP).equals("STEP2")) {
            request.withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()));
        } else if (context.getRequestData().get(CONTEXT_DATA_STEP).equals("STEP3")) {
            request.withPaymentFormContext(MockUtils.aPaymentFormContext());
        }
        return request.build();
    }

    @Test
    void fullPaymentTest() {
        // ConfigurationService Test
        ContractParametersCheckRequest contractParametersCheckRequest = MockUtils.aContractParametersCheckRequest();
        Map<String, String> errors = configurationService.check(contractParametersCheckRequest);
        Assertions.assertTrue(errors.isEmpty());

        //  Step 1 (create the transaction)
        PaymentRequest requestStep1 = createDefaultPaymentRequest();
        PaymentResponse responseStep1 = paymentService.paymentRequest(requestStep1);

        //  the response should be a form updated to enter the payment ticket
        Assertions.assertEquals(PaymentResponseFormUpdated.class, responseStep1.getClass());
        PaymentResponseFormUpdated responseFormUpdated = (PaymentResponseFormUpdated) responseStep1;
        Assertions.assertEquals(STEP2, responseFormUpdated.getRequestContext().getRequestData().get(CONTEXT_DATA_STEP));
        String partnerTransactionId = responseFormUpdated.getRequestContext().getRequestData().get(NUMTRANSAC);

        // Step 2 (add the payment ticket to the transaction previously created)
        Map<String, String> parameters = new HashMap<>();
        parameters.put(FormConfigurationKeys.CABTITRE, "25394000194000103135057060172010123902001000");

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withPaymentFormParameter(parameters)
                .build();

        PaymentRequest request2 = MockUtils.aPaylinePaymentRequestBuilder()
                .withRequestContext(responseFormUpdated.getRequestContext())
                .withPaymentFormContext(context)
                .build();
        PaymentResponse responseStep2 = paymentService.paymentRequest(request2);
        // the response should be a Success
        Assertions.assertEquals(PaymentResponseSuccess.class, responseStep2.getClass());
        PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) responseStep2;

        Assertions.assertEquals(partnerTransactionId, responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("1", responseSuccess.getStatusCode());

//
//        PaymentRequest request3 = createPaymentRequest(response2.getRequestContext());
//        PaymentResponse paymentResponseSetFinTransac = paymentService.paymentRequest(request3);
//        Assertions.assertEquals(PaymentResponseSuccess.class, paymentResponseSetFinTransac.getClass());
    }


    public PaymentRequest createDefaultPaymentRequest() {
        return MockUtils.aPaylinePaymentRequest();
    }
}
