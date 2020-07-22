package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseFailure;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

class RefundServiceImplTest {
    @InjectMocks
    RefundServiceImpl service = new RefundServiceImpl();

    @Mock
    HttpService httpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    private static final String GOOD_TOKEN_RESPONSE = "{\n" +
            "    \"error\": 0,\n" +
            "    \"message\": \"\",\n" +
            "    \"token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IktJQUJJX1dTIiwidHlwZSI6IkpXVCJ9.eyJleHAiOjE1OTQ2NTE3NDYsImp0aSI6IjgxOWVkOWRjN2Y4NTA3NWU3NzEwNDMwNzJhNmU4NjgxIn0.qWmcFoawdaKz86xxNBlexny7IPLku3Wlxw_-7yaZPR2bMX0OS8MXpGGXXosJhyHUZV6P5p8yf5xEf_dcmTTfgA\"\n" +
            "}";

    private static final String BAD_TOKEN_RESPONSE = "{\n" +
            "    \"error\": 78,\n" +
            "    \"message\": \"Erreur authentification\",\n" +
            "    \"detail\": \"Le login ou le password n’est pas reconnu\"\n" +
            "}";

    private static final String GOOD_CARD_RESPONSE = "{\n" +
            "    \"error\": 0,\n" +
            "    \"message\": \"\",\n" +
            "    \"cartes\": {\n" +
            "        \"cardid\": \"2539400019400018828372289117202107203902100000\",\n" +
            "        \"cardid2\": \"8283722891\",\n" +
            "        \"cardcvv\": \"\",\n" +
            "        \"montant\": 1000\n" +
            "    }\n" +
            "}";

    private static final String BAD_CARD_RESPONSE = "{\n" +
            "    \"error\": -118,\n" +
            "    \"message\": \" Produit Refuse par l’enseigne \",\n" +
            "    \"detail\": []\n" +
            "}";

    private static final String GOOD_MAIL_RESPONSE = "{\n" +
            "    \"error\": 0,\n" +
            "    \"message\": \"\"\n" +
            "}";

    private static final String BAD_MAIL_RESPONSE = "{\n" +
            "    \"error\": -115,\n" +
            "    \"message\": \"Token expiré \",\n" +
            "    \"detail\": []\n" +
            "}";

    @Test
    void refundRequestOK() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(GOOD_TOKEN_RESPONSE);
        SetCreateCard cardResponse = SetCreateCard.fromJson(GOOD_CARD_RESPONSE);
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(GOOD_MAIL_RESPONSE);

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.doReturn(mailResponse).when(httpService).setGenCardMail(any(), any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);


        // assertions
        Assertions.assertEquals(RefundResponseSuccess.class, response.getClass());
        RefundResponseSuccess responseSuccess = (RefundResponseSuccess) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("0", responseSuccess.getStatusCode());
    }

    @Test
    void refundRequestKOToken() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(BAD_TOKEN_RESPONSE);

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.verify(httpService, never()).setCreateCard(any(), any(), any());
        Mockito.verify(httpService, never()).setGenCardMail(any(), any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);


        // assertions
        Assertions.assertEquals(RefundResponseFailure.class, response.getClass());
        RefundResponseFailure responseFailure = (RefundResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Erreur authentification", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }

    @Test
    void refundRequestKOCard() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(GOOD_TOKEN_RESPONSE);
        SetCreateCard cardResponse = SetCreateCard.fromJson(BAD_CARD_RESPONSE);

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.verify(httpService, never()).setGenCardMail(any(), any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);


        // assertions
        Assertions.assertEquals(RefundResponseFailure.class, response.getClass());
        RefundResponseFailure responseFailure = (RefundResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Produit Refuse par l’enseigne", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }

    @Test
    void refundRequestKOMail() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(GOOD_TOKEN_RESPONSE);
        SetCreateCard cardResponse = SetCreateCard.fromJson(GOOD_CARD_RESPONSE);
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(BAD_MAIL_RESPONSE);

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.doReturn(mailResponse).when(httpService).setGenCardMail(any(), any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);


        // assertions
        Assertions.assertEquals(RefundResponseFailure.class, response.getClass());
        RefundResponseFailure responseFailure = (RefundResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Token expiré", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }


    @Test
    void refundRequestPluginException() {
        String errorMessage = "foo";
        FailureCause cause = FailureCause.FRAUD_DETECTED;

        // create mock
        Mockito.doThrow(new PluginException(errorMessage, cause)).when(httpService).getAuthToken(any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);

        // assertions
        Assertions.assertEquals(RefundResponseFailure.class, response.getClass());
        RefundResponseFailure responseFailure = (RefundResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
        Assertions.assertEquals(cause, responseFailure.getFailureCause());
    }

    @Test
    void refundRequestRuntimeException() {
        String errorMessage = "foo";

        // create mock
        Mockito.doThrow(new NullPointerException(errorMessage)).when(httpService).getAuthToken(any(), any());

        // call method
        RefundRequest request = MockUtils.aPaylineRefundRequest();
        RefundResponse response = service.refundRequest(request);

        // assertions
        Assertions.assertEquals(RefundResponseFailure.class, response.getClass());
        RefundResponseFailure responseFailure = (RefundResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INTERNAL_ERROR, responseFailure.getFailureCause());
    }


    @Test
    void canMultiple() {
        Assertions.assertTrue(service.canMultiple());
    }

    @Test
    void canPartial() {
        Assertions.assertTrue(service.canPartial());
    }

    @Test
    void responseFailure() {
    }
}