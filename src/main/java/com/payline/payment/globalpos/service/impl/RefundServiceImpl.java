package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.bean.request.CreateCardBody;
import com.payline.payment.globalpos.bean.request.LoginBody;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.PluginUtils;
import com.payline.payment.globalpos.utils.constant.ContractConfigurationKeys;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseFailure;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.RefundService;
import org.apache.logging.log4j.Logger;


public class RefundServiceImpl implements RefundService {
    private static final Logger LOGGER = LogManager.getLogger(RefundServiceImpl.class);
    private HttpService httpService = HttpService.getInstance();


    @Override
    public RefundResponse refundRequest(RefundRequest request) {
        RefundResponse refundResponse;
        String partnerTransactionId = request.getPartnerTransactionId();
        try {
            final RequestConfiguration configuration = RequestConfiguration.build(request);

            // get the access token
            String id = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.ID).getValue();
            String password = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.PASSWORD).getValue();
            String guid = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.GUID).getValue();

            LoginBody loginBody = new LoginBody(id, password, guid);
            GetAuthToken tokenResponse = httpService.getAuthToken(configuration, loginBody);
            if (tokenResponse.isOk()) {
                return  askForNewTicket(configuration, tokenResponse, request, partnerTransactionId);
            } else {
                refundResponse = responseFailure(partnerTransactionId, tokenResponse);
            }
        } catch (PluginException e) {
            refundResponse = e.toRefundResponseFailureBuilder().withPartnerTransactionId(partnerTransactionId).build();
        } catch (RuntimeException e) {
            refundResponse = RefundResponseFailure.RefundResponseFailureBuilder
                    .aRefundResponseFailure()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withErrorCode(PluginUtils.truncate(e.getMessage(), 50))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

        return refundResponse;
    }

    @Override
    public boolean canMultiple() {
        return true;
    }

    @Override
    public boolean canPartial() {
        return true;
    }


    /**
     * Create the ResponseFailure
     *
     * @param response the responseError return by the API GlobalPOS
     * @return PaymentResponseFailure
     */
    private RefundResponseFailure responseFailure(String partnerTransactionId, JsonBeanResponse response) {
        LOGGER.info("Failure While calling API:{}", response);
        return RefundResponseFailure.RefundResponseFailureBuilder
                .aRefundResponseFailure()
                .withPartnerTransactionId(partnerTransactionId)
                .withErrorCode(PluginUtils.truncate(response.getMessage().trim(), 50))
                .withFailureCause(PluginUtils.getFailureCause(response.getError()))
                .build();
    }


    private RefundResponse askForValidation(RequestConfiguration configuration, SetCreateCard cardResponse, String token, String partnerTransactionId){
        String cardId = cardResponse.getCard().getCardId();
        RefundResponse refundResponse;

        // ask for validation and email sending
        JsonBeanResponse sendMailResponse = httpService.setGenCardMail(configuration, token, cardId);
        if (sendMailResponse.isOk()) {
            refundResponse = RefundResponseSuccess.RefundResponseSuccessBuilder
                    .aRefundResponseSuccess()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withStatusCode("0") // sendMailResponse.error = 0 when OK
                    .build();
        } else {
            refundResponse = responseFailure(partnerTransactionId, sendMailResponse);
        }

        return refundResponse;
    }

    private RefundResponse askForNewTicket(RequestConfiguration configuration, GetAuthToken tokenResponse, RefundRequest request, String partnerTransactionId){
        RefundResponse refundResponse;

        String token = tokenResponse.getToken();
        String date = PluginUtils.computeDate();
        String email = request.getBuyer().getEmail();
        String magCaisse = PluginUtils.createStoreId(
                configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.CODEMAGASIN).getValue()
                , configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.NUMEROCAISSE).getValue()
        );

        CreateCardBody createCardBody = CreateCardBody.builder()
                .action(CreateCardBody.Action.CREATION.name())
                .dateTransac(date)
                .email(email)
                .magCaisse(magCaisse)
                .montant(request.getAmount().getAmountInSmallestUnit().intValue())
                .numTransac(request.getTransactionId())
                .typeTitre(CreateCardBody.Title.TITLE940001.getName())
                .build();

        // ask for a new payment ticket
        SetCreateCard cardResponse = httpService.setCreateCard(configuration, token, createCardBody);
        if (cardResponse.isOk()) {
            refundResponse = askForValidation(configuration, cardResponse, token, partnerTransactionId);
        } else {
            refundResponse = responseFailure(partnerTransactionId, cardResponse);
        }
        return refundResponse;
    }
}
