package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.notification.request.NotificationRequest;
import com.payline.pmapi.bean.notification.response.NotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.IgnoreNotificationResponse;
import com.payline.pmapi.bean.payment.request.NotifyTransactionStatusRequest;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.NotificationService;
import org.apache.logging.log4j.Logger;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger LOGGER = LogManager.getLogger(NotificationServiceImpl.class);
    private HttpService httpService = HttpService.getInstance();

    @Override
    public NotificationResponse parse(NotificationRequest request) {
        return new IgnoreNotificationResponse();
    }

    @Override
    public void notifyTransactionStatus(NotifyTransactionStatusRequest request) {
        final RequestConfiguration configuration = RequestConfiguration.build(request);

        // verify if all needed data are present
        if (request.getTransactionSatus() == null) {
            throw new InvalidDataException("Transaction status is missing");
        }


        // extract needed data
        String partnerTransactionId = request.getPartnerTransactionId();
        PaymentServiceImpl.STATUS status = request.getTransactionSatus().equals(NotifyTransactionStatusRequest.TransactionStatus.SUCCESS) ?
                PaymentServiceImpl.STATUS.COMMIT : PaymentServiceImpl.STATUS.ROLLBACK;


        // finalize the transaction
        String name = status.name();
        LOGGER.info("Call Global pos API to finalize transaction {} with status:{}", partnerTransactionId, name);

        httpService.manageTransact(configuration,partnerTransactionId,status.name(), TransactionType.FINALISE_TRANSACTION);
    }
}
