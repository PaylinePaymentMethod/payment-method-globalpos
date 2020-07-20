package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.notification.response.impl.IgnoreNotificationResponse;
import com.payline.pmapi.bean.payment.request.NotifyTransactionStatusRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl service = new NotificationServiceImpl();

    @Mock
    private HttpService httpService;

    private String partnerTransactionId = "123456";

    private NotifyTransactionStatusRequest.NotifyTransactionStatusRequestBuilder baseRequestBuilder = NotifyTransactionStatusRequest.NotifyTransactionStatusRequestBuilder
            .aNotifyTransactionStatusRequest()
            .withContractConfiguration(MockUtils.aContractConfiguration())
            .withPartnerConfiguration(MockUtils.aPartnerConfiguration())
            .withEnvironment(MockUtils.anEnvironment())
            .withPartnerTransactionId(partnerTransactionId)
            .withAmount(MockUtils.aPaylineAmount());


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void parse() {
        Assertions.assertEquals(IgnoreNotificationResponse.class, service.parse(null).getClass());
    }

    @Test
    void notifyTransactionStatusCOMMIT() {
        NotifyTransactionStatusRequest request = baseRequestBuilder
                .withTransactionSatus(NotifyTransactionStatusRequest.TransactionStatus.SUCCESS)
                .build();

        service.notifyTransactionStatus(request);
        Mockito.verify(httpService, Mockito.times(1))
                .manageTransact(any(), eq(partnerTransactionId), eq(PaymentServiceImpl.STATUS.COMMIT.name()), eq(TransactionType.FINALISE_TRANSACTION));

    }

    @Test
    void notifyTransactionStatusROLLBACK() {
        NotifyTransactionStatusRequest request = baseRequestBuilder
                .withTransactionSatus(NotifyTransactionStatusRequest.TransactionStatus.FAIL)
                .build();

        service.notifyTransactionStatus(request);
        Mockito.verify(httpService, Mockito.times(1))
                .manageTransact(any(), eq(partnerTransactionId), eq(PaymentServiceImpl.STATUS.ROLLBACK.name()),eq(TransactionType.FINALISE_TRANSACTION));
    }
}