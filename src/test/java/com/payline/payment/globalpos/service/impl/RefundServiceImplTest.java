package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.constant.RequestContextKeys;
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

    @Test
    void refundRequestOK() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getGOOD_CARD_RESPONSE());
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(MockUtils.getGOOD_MAIL_RESPONSE());

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
        Assertions.assertNotNull(responseSuccess.getMiscellaneous());
        Assertions.assertEquals("2539400019400018828372289117202107203902100000",responseSuccess.getMiscellaneous().get(RequestContextKeys.VOUCHER));
    }

    @Test
    void refundRequestKOToken() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getBAD_TOKEN_RESPONSE());

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
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getBAD_CARD_RESPONSE());

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
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getGOOD_CARD_RESPONSE());
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(MockUtils.getBAD_MAIL_RESPONSE());

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

}