package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.reset.request.ResetRequest;
import com.payline.pmapi.bean.reset.response.ResetResponse;
import com.payline.pmapi.bean.reset.response.impl.ResetResponseFailure;
import com.payline.pmapi.bean.reset.response.impl.ResetResponseSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

class ResetServiceImplTest {
    @InjectMocks
    private ResetServiceImpl service = new ResetServiceImpl();

    @Mock
    HttpService httpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void resetRequestOK() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getGOOD_CARD_RESPONSE());
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(MockUtils.getGOOD_MAIL_RESPONSE());

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.doReturn(mailResponse).when(httpService).setGenCardMail(any(), any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);


        // assertions
        Assertions.assertEquals(ResetResponseSuccess.class, response.getClass());
        ResetResponseSuccess responseSuccess = (ResetResponseSuccess) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("0", responseSuccess.getStatusCode());
    }

    @Test
    void resetRequestKOToken() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getBAD_TOKEN_RESPONSE());

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.verify(httpService, never()).setCreateCard(any(), any(), any());
        Mockito.verify(httpService, never()).setGenCardMail(any(), any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);


        // assertions
        Assertions.assertEquals(ResetResponseFailure.class, response.getClass());
        ResetResponseFailure responseFailure = (ResetResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Erreur authentification", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }

    @Test
    void resetRequestKOCard() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getBAD_CARD_RESPONSE());

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.verify(httpService, never()).setGenCardMail(any(), any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);


        // assertions
        Assertions.assertEquals(ResetResponseFailure.class, response.getClass());
        ResetResponseFailure responseFailure = (ResetResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Produit Refuse par l’enseigne", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }

    @Test
    void resetRequestKOMail() {

        // create mock
        GetAuthToken tokenResponse = GetAuthToken.fromJson(MockUtils.getGOOD_TOKEN_RESPONSE());
        SetCreateCard cardResponse = SetCreateCard.fromJson(MockUtils.getGOOD_CARD_RESPONSE());
        JsonBeanResponse mailResponse = JsonBeanResponse.fromJson(MockUtils.getBAD_MAIL_RESPONSE());

        Mockito.doReturn(tokenResponse).when(httpService).getAuthToken(any(), any());
        Mockito.doReturn(cardResponse).when(httpService).setCreateCard(any(), any(), any());
        Mockito.doReturn(mailResponse).when(httpService).setGenCardMail(any(), any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);


        // assertions
        Assertions.assertEquals(ResetResponseFailure.class, response.getClass());
        ResetResponseFailure responseFailure = (ResetResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals("Token expiré", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }


    @Test
    void resetRequestPluginException() {
        String errorMessage = "foo";
        FailureCause cause = FailureCause.FRAUD_DETECTED;

        // create mock
        Mockito.doThrow(new PluginException(errorMessage, cause)).when(httpService).getAuthToken(any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);

        // assertions
        Assertions.assertEquals(ResetResponseFailure.class, response.getClass());
        ResetResponseFailure responseFailure = (ResetResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
        Assertions.assertEquals(cause, responseFailure.getFailureCause());
    }

    @Test
    void resetRequestRuntimeException() {
        String errorMessage = "foo";

        // create mock
        Mockito.doThrow(new NullPointerException(errorMessage)).when(httpService).getAuthToken(any(), any());

        // call method
        ResetRequest request = MockUtils.aPaylineResetRequest();
        ResetResponse response = service.resetRequest(request);

        // assertions
        Assertions.assertEquals(ResetResponseFailure.class, response.getClass());
        ResetResponseFailure responseFailure = (ResetResponseFailure) response;

        Assertions.assertEquals(MockUtils.getPARTNER_TRANSACTIONID(), responseFailure.getPartnerTransactionId());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INTERNAL_ERROR, responseFailure.getFailureCause());
    }
}