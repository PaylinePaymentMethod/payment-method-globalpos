package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.bean.response.APIResponseError;
import com.payline.payment.globalpos.bean.response.GetTitreDetailTransac;
import com.payline.payment.globalpos.bean.response.GetTransac;
import com.payline.payment.globalpos.bean.response.SetFinTransac;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.AmountParse;
import com.payline.payment.globalpos.utils.PluginUtils;
import com.payline.payment.globalpos.utils.constant.ContractConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.FormConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.RequestContextKeys;
import com.payline.payment.globalpos.utils.form.FormUtils;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.payline.payment.globalpos.utils.constant.RequestContextKeys.STEP_RETRY;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);
    private HttpService httpService = HttpService.getInstance();
    private FormUtils formUtils = FormUtils.getInstance();

    // the status for finalize the transaction can only be COMMIT or ROLLBACK
    public enum STATUS {
        ROLLBACK,
        COMMIT
    }

    @Override
    public PaymentResponse paymentRequest(PaymentRequest request) {
        PaymentResponse response;
        try {

            if (request.getRequestContext() == null) {
                throw new InvalidDataException("RequestContext is missing");
            }

            // check if it's first try or a retry
            String step = request.getRequestContext().getRequestData().get(RequestContextKeys.CONTEXT_DATA_STEP);
            if (PluginUtils.isEmpty(step)) {
                // ask for begin a transaction, and take the PartnerTransactionId
                // then add the ticket to the transaction
                response = askForNewTransaction(request);
            } else if (STEP_RETRY.equals(step)) {
                // Transaction has already been created, just add payment ticket to it
                String partnerTransactionId = request.getRequestContext().getRequestData().get(RequestContextKeys.NUMTRANSAC);

                if (PluginUtils.isEmpty(partnerTransactionId)) {
                    String errorMessage = "Retry request must contain a partnerTransactionId";
                    LOGGER.error(errorMessage);
                    throw new InvalidDataException(errorMessage);
                }
                response = askForAddingTicket(request, partnerTransactionId);
            } else {
                // should never append
                String errorMessage = "Unknown step";
                LOGGER.error(errorMessage);
                response = PaymentResponseFailure.PaymentResponseFailureBuilder
                        .aPaymentResponseFailure()
                        .withErrorCode(errorMessage)
                        .withFailureCause(FailureCause.INVALID_DATA)
                        .build();
            }
        } catch (PluginException e) {
            LOGGER.error(e.getMessage(), e);
            response = e.toPaymentResponseFailureBuilder()
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            response = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
        return response;
    }

    /**
     * Step1 of the transaction, ask the NumTransaction to GlobalPOS and add a paymentTicket to the transaction
     *
     * @param request the PaymentRequest
     * @return PaymentResponseFormUpdated to ask the CabTitre to the chopper
     */
    public PaymentResponse askForNewTransaction(PaymentRequest request) {
        final RequestConfiguration configuration = RequestConfiguration.build(request);

        // verify if all needed data are present
        if (PluginUtils.isEmpty(request.getTransactionId())) {
            throw new InvalidDataException("TransactionId is missing");
        }

        if (request.getPaymentFormContext() == null || request.getPaymentFormContext().getPaymentFormParameter() == null
                || request.getPaymentFormContext().getPaymentFormParameter().get(FormConfigurationKeys.CABTITRE) == null) {
            throw new InvalidDataException("issues with the PaymentFormContext");
        }

        if (request.getAmount() == null || request.getAmount().getAmountInSmallestUnit() == null
                || request.getAmount().getCurrency() == null) {
            throw new InvalidDataException("issues with the requestAmount");
        }

        // extract needed data to create a new transaction
        String guid = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.GUID).getValue();
        String storeCode = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.CODEMAGASIN).getValue();
        String checkoutNumber = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.NUMEROCAISSE).getValue();

        // call API to ask for a new transaction
        String stringResponse = httpService.getTransact(configuration, guid, storeCode, checkoutNumber, request.getTransactionId());
        GetTransac response = GetTransac.fromXml(stringResponse);

        // 1 if response is OK, else is null
        if ("1".equals(response.getCodeErreur())) {
            // The transaction has been created, now add it the payment ticket
            return askForAddingTicket(request, response.getNumTransac());

        } else {
            // something is wrong return a PaymentResponseFailure
            return this.responseFailure(APIResponseError.fromXml(stringResponse), null);
        }
    }


    /**
     * Step 2 of the transaction, add the received payment ticket to the transaction and then return different responses from the ticket amount
     * if ticket amount equal the transaction amount: validate the transaction and return a success
     * if ticket amount is below the transaction amount: ask for a complementary payment (card)
     * if ticket amount is above the transaction amount: cancel it and re-ask for another ticket
     *
     * @param request the PaymentRequest
     * @return PaymentResponseFailure if the amount is incorrect, or if the date is expired
     * PaymentResponseFormUpdated else
     */
    public PaymentResponse askForAddingTicket(PaymentRequest request, String partnerTransactionId) {
        final RequestConfiguration configuration = RequestConfiguration.build(request);

        if(request.getPaymentFormContext().getPaymentFormParameter().get(FormConfigurationKeys.CABTITRE) == null){
            throw new InvalidDataException("CABTITRE is missing in the payment request");
        }

        // extract needed data
        String cabTitre = request.getPaymentFormContext().getPaymentFormParameter().get(FormConfigurationKeys.CABTITRE).trim();
        BigDecimal paylineAmount = AmountParse.splitDecimal(request.getAmount());

        // add a payment ticket to the transaction created in step1
        String stringResponse = httpService.manageTransact(configuration, partnerTransactionId, cabTitre, TransactionType.DETAIL_TRANSACTION);
        GetTitreDetailTransac response = GetTitreDetailTransac.fromXml(stringResponse);

        PaymentResponse paymentResponse;

        if (response == null || response.getCodeErreur() == null) {
            paymentResponse = this.responseFailure(APIResponseError.fromXml(stringResponse), partnerTransactionId);
        } else if (response.getMontant() == null) {
            throw new InvalidDataException("Amount is missing on the payment ticket response");
        } else {
            BigDecimal gpAmount = new BigDecimal(response.getMontant());

            switch (gpAmount.compareTo(paylineAmount)) {
                case 0:
                    // every thing is OK, finalize transaction

                    stringResponse = httpService.manageTransact(configuration, partnerTransactionId, STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION);
                    paymentResponse = handleSetFinTransacResponse(stringResponse, partnerTransactionId, null);
                    break;
                case -1:
                    // a complementary payment is needed

                    // finalize the transaction (even if not fully paid) if an error occur, the resetService will create a new paid ticket as a refund
                    Amount reservedAmount = new Amount(AmountParse.createBigInteger(response.getMontant(), request.getAmount().getCurrency()), request.getAmount().getCurrency());
                    stringResponse = httpService.manageTransact(configuration, partnerTransactionId, STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION);
                    paymentResponse = handleSetFinTransacResponse(stringResponse, partnerTransactionId, reservedAmount);
                    break;
                case 1:
                default:
                    // the payment ticket is too big,cancel it and return the payment ticket form again (with an additional error message)

                    // cancel the payment ticket
                    stringResponse = httpService.manageTransact(configuration, partnerTransactionId, response.getId(), TransactionType.CANCEL_TRANSACTION);
                    paymentResponse = handleSetAnnulTransacResponse(stringResponse, partnerTransactionId, request, gpAmount.toString());
                    break;
            }
        }


        return paymentResponse;
    }


    /**
     * Create the right PaymentResponse from the setFinTransac response
     *
     * @param stringResponse       setFinTransac response
     * @param partnerTransactionId the partnerTransactionId
     * @return
     */
    private PaymentResponse handleSetFinTransacResponse(String stringResponse, String partnerTransactionId, Amount reservedAmount) {
        PaymentResponse paymentResponse;

        SetFinTransac response = SetFinTransac.fromXml(stringResponse);

        if (response == null || response.getCodeErreur() == null) {
            paymentResponse = this.responseFailure(APIResponseError.fromXml(stringResponse), partnerTransactionId);
        } else {
            PaymentResponseSuccess.PaymentResponseSuccessBuilder responseSuccessBuilder = PaymentResponseSuccess.PaymentResponseSuccessBuilder
                    .aPaymentResponseSuccess()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withTransactionDetails(new EmptyTransactionDetails())
                    .withStatusCode(response.getCodeErreur());

            if (reservedAmount != null) {
                responseSuccessBuilder.withReservedAmount(reservedAmount);
            }

            paymentResponse = responseSuccessBuilder.build();
        }
        return paymentResponse;
    }


    private PaymentResponse handleSetAnnulTransacResponse(String stringResponse, String partnerTransactionId, PaymentRequest request, String amount) {
        PaymentResponse paymentResponse;

        APIResponseError response = APIResponseError.fromXml(stringResponse);

        if (response.getError() == 0) {
            // if cancellation is OK, return a new form to enter a new payment ticket

            // create the form
            Locale locale = request.getLocale();
            String currency =  request.getAmount().getCurrency().getSymbol(locale);
            PaymentFormConfigurationResponse paymentFormConfigurationResponse = formUtils.createRetryForm( locale, amount, currency);

            // create RequestContext for the next step (STEP2 again)
            Map<String, String> requestContextMap = new HashMap<>();
            requestContextMap.put(RequestContextKeys.CONTEXT_DATA_STEP, STEP_RETRY);
            requestContextMap.put(RequestContextKeys.NUMTRANSAC, partnerTransactionId);

            RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext()
                    .withRequestData(requestContextMap)
                    .build();

            // return the form to ask for a new payment ticket
            paymentResponse = PaymentResponseFormUpdated.PaymentResponseFormUpdatedBuilder.aPaymentResponseFormUpdated()
                    .withPaymentFormConfigurationResponse(paymentFormConfigurationResponse)
                    .withRequestContext(requestContext)
                    .build();
        } else {
            paymentResponse = this.responseFailure(response, partnerTransactionId);
        }

        return paymentResponse;
    }

    /**
     * Create the ResponseFailure
     *
     * @param response the responseError return by the API GlobalPOS
     * @return PaymentResponseFailure
     */
    public PaymentResponseFailure responseFailure(APIResponseError response, String partnerTransactionId) {
        LOGGER.info("Failure While calling API:{}", response);
        return PaymentResponseFailure.PaymentResponseFailureBuilder.aPaymentResponseFailure()
                .withErrorCode(PluginUtils.truncate(response.getMessage(), 50))
                .withFailureCause(PluginUtils.getFailureCause(response.getError()))
                .withPartnerTransactionId(partnerTransactionId)
                .build();
    }
}
