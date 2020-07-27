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
import com.payline.pmapi.bean.reset.request.ResetRequest;
import com.payline.pmapi.bean.reset.response.ResetResponse;
import com.payline.pmapi.bean.reset.response.impl.ResetResponseFailure;
import com.payline.pmapi.bean.reset.response.impl.ResetResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.ResetService;
import org.apache.logging.log4j.Logger;


public class ResetServiceImpl implements ResetService {
    private static final Logger LOGGER = LogManager.getLogger(ResetServiceImpl.class);
    private HttpService httpService = HttpService.getInstance();


    @Override
    public ResetResponse resetRequest(ResetRequest request) {
        ResetResponse resetResponse;
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
                resetResponse =   askForNewTicket(configuration, tokenResponse, request, partnerTransactionId);
            } else {
                resetResponse = responseFailure(partnerTransactionId, tokenResponse);
            }
        } catch (PluginException e) {
            resetResponse = e.toResetResponseFailureBuilder().withPartnerTransactionId(partnerTransactionId).build();
        } catch (RuntimeException e) {
            resetResponse = ResetResponseFailure.ResetResponseFailureBuilder
                    .aResetResponseFailure()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withErrorCode(PluginUtils.truncate(e.getMessage(), 50))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

        return resetResponse;
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
    private ResetResponseFailure responseFailure(String partnerTransactionId, JsonBeanResponse response) {
        LOGGER.info("Failure While calling API:{}", response);
        return ResetResponseFailure.ResetResponseFailureBuilder
                .aResetResponseFailure()
                .withPartnerTransactionId(partnerTransactionId)
                .withErrorCode(PluginUtils.truncate(response.getMessage().trim(), 50))
                .withFailureCause(PluginUtils.getFailureCause(response.getError()))
                .build();
    }


    private ResetResponse askForValidation(RequestConfiguration configuration, SetCreateCard cardResponse, String token, String partnerTransactionId){
        String cardId = cardResponse.getCard().getCardId();
        ResetResponse resetResponse;

        // ask for validation and email sending
        JsonBeanResponse sendMailResponse = httpService.setGenCardMail(configuration, token, cardId);
        if (sendMailResponse.isOk()) {
            resetResponse = ResetResponseSuccess.ResetResponseSuccessBuilder
                    .aResetResponseSuccess()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withStatusCode("0") // sendMailResponse.error = 0 when OK
                    .build();
        } else {
            resetResponse = responseFailure(partnerTransactionId, sendMailResponse);
        }

        return resetResponse;
    }

    private ResetResponse askForNewTicket(RequestConfiguration configuration, GetAuthToken tokenResponse, ResetRequest request, String partnerTransactionId){
        ResetResponse resetResponse;

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
            return askForValidation(configuration, cardResponse, token, partnerTransactionId);
        } else {
            resetResponse = responseFailure(partnerTransactionId, cardResponse);
        }
        return resetResponse;
    }
}
