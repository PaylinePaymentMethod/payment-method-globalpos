package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.utils.AmountParse;
import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.bean.response.APIResponseError;
import com.payline.payment.globalpos.bean.response.GetTitreDetailTransac;
import com.payline.payment.globalpos.bean.response.GetTransac;
import com.payline.payment.globalpos.bean.response.SetFinTransac;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.PluginUtils;
import com.payline.payment.globalpos.utils.constant.ContractConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.FormConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.RequestContextKeys;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.payment.globalpos.utils.i18n.I18nService;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.bean.paymentform.bean.field.*;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import static com.payline.payment.globalpos.utils.constant.RequestContextKeys.STEP2;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);
    private HttpService httpService = HttpService.getInstance();

    private I18nService i18n = I18nService.getInstance();

    // the status for finalize the transaction can only be COMMIT or ROLLBACK
    public enum STATUS {
        ROLLBACK,
        COMMIT
    }

    @Override
    public PaymentResponse paymentRequest(PaymentRequest request) {
        try {
            if (request.getRequestContext() == null) {
                throw new InvalidDataException("RequestContext is missing");
            }
            String step = request.getRequestContext().getRequestData().get(RequestContextKeys.CONTEXT_DATA_STEP);
            if (PluginUtils.isEmpty(step)) {
                // ask for begin a transaction, and take the PartnerTransactionId
                // return a PaymentResponseFormUpdated() to ask the payment ticket
                return step1(request);
            } else if (STEP2.equals(step)) {
                // add the received payment ticket to the transaction and then return different responses from the ticket amount
                return step2(request);
            } else {
                // should never append
                String errorMessage = "Unknown step";
                LOGGER.error(errorMessage);
                return PaymentResponseFailure.PaymentResponseFailureBuilder
                        .aPaymentResponseFailure()
                        .withErrorCode(errorMessage)
                        .withFailureCause(FailureCause.INVALID_DATA)
                        .build();
            }
        } catch (PluginException e) {
            LOGGER.error(e.getMessage(), e);
            return e.toPaymentResponseFailureBuilder()
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
    }

    /**
     * Step1 of the transaction, ask the NumTransaction to GlobalPOS
     *
     * @param request the PaymentRequest
     * @return PaymentResponseFormUpdated to ask the CabTitre to the chopper
     */
    public PaymentResponse step1(PaymentRequest request) {
        final RequestConfiguration configuration = RequestConfiguration.build(request);
        // should never append
        if (PluginUtils.isEmpty(request.getTransactionId())) {
            throw new InvalidDataException("TransactionId is missing");
        }

        String guid = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.GUID).getValue();
        String storeCode = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.CODEMAGASIN).getValue();
        String checkoutNumber = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.NUMEROCAISSE).getValue();

        String stringResponse = httpService.getTransact(configuration, guid, storeCode, checkoutNumber, request.getTransactionId());
        GetTransac response = GetTransac.fromXml(stringResponse);

        // 1 if response is OK, else is null
        if ("1".equals(response.getCodeErreur())) {
            // The transaction has been created, return a payment ticket Form

            // create the form
            PaymentFormConfigurationResponse paymentFormConfigurationResponse = createSimpleForm(request.getLocale());

            // create RequestContext for the next step (Step2)
            Map<String, String> requestContextMap = new HashMap<>();
            requestContextMap.put(RequestContextKeys.CONTEXT_DATA_STEP, STEP2);
            requestContextMap.put(RequestContextKeys.NUMTRANSAC, response.getNumTransac());
            RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext()
                    .withRequestData(requestContextMap)
                    .build();

            return PaymentResponseFormUpdated.PaymentResponseFormUpdatedBuilder.aPaymentResponseFormUpdated()
                    .withPaymentFormConfigurationResponse(paymentFormConfigurationResponse)
                    .withRequestContext(requestContext)
                    .build();
        } else {
            // something is wrong return a PaymentResponseFailure
            return this.responseFailure(APIResponseError.fromXml(stringResponse));
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
    public PaymentResponse step2(PaymentRequest request) {
        final RequestConfiguration configuration = RequestConfiguration.build(request);

        // verify if all needed data are present
        if (request.getPaymentFormContext() == null || request.getPaymentFormContext().getPaymentFormParameter() == null
                || request.getPaymentFormContext().getPaymentFormParameter().get(FormConfigurationKeys.CABTITRE) == null) {
            throw new InvalidDataException("issues with the PaymentFormContext");
        }
        if (request.getRequestContext() == null ||
                request.getRequestContext().getRequestData().get(RequestContextKeys.NUMTRANSAC) == null) {
            throw new InvalidDataException("issues with the numTransac in the requestContext");
        }
        if (request.getAmount() == null || request.getAmount().getAmountInSmallestUnit() == null
                || request.getAmount().getCurrency() == null) {
            throw new InvalidDataException("issues with the requestAmount");
        }

        // extract needed data
        String partnerTransactionId = request.getRequestContext().getRequestData().get(RequestContextKeys.NUMTRANSAC);
        String cabTitre = request.getPaymentFormContext().getPaymentFormParameter().get(FormConfigurationKeys.CABTITRE);
        BigDecimal paylineAmount = AmountParse.splitDecimal(request.getAmount());

        // add a payment ticket to the transaction created in step1

        String stringResponse = httpService.manageTransact(configuration, partnerTransactionId, cabTitre, TransactionType.DETAIL_TRANSACTION);
        GetTitreDetailTransac response = GetTitreDetailTransac.fromXml(stringResponse);

        PaymentResponse paymentResponse;

        if (response == null || response.getCodeErreur() == null) {
            paymentResponse = this.responseFailure(APIResponseError.fromXml(stringResponse));
        } else if (response.getMontant() == null) {
            throw new InvalidDataException("Amount is missing on the check");
        } else {
            BigDecimal gpAmount = new BigDecimal(response.getMontant());

            switch (gpAmount.compareTo(paylineAmount)) {
                case 0:
                    // every thing is OK, finalize transaction

                    stringResponse = httpService.manageTransact(configuration, partnerTransactionId, STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION);
                    paymentResponse = handleSetFinTransacResponse(stringResponse, partnerTransactionId);
                    break;
                case -1:
                    // a complementary payment is needed, return paymentResponseSuccess with a reservedAmount
                    Amount reservedAmount = new Amount(AmountParse.createBigInteger(response.getMontant(), request.getAmount().getCurrency()), request.getAmount().getCurrency());


                    paymentResponse = PaymentResponseSuccess.PaymentResponseSuccessBuilder
                            .aPaymentResponseSuccess()
                            .withStatusCode(response.getCodeErreur())
                            .withPartnerTransactionId(partnerTransactionId)
                            .withReservedAmount(reservedAmount)
                            .withTransactionDetails(new EmptyTransactionDetails())
                            .build();
                    break;
                case 1:
                default:
                    // the payment ticket is too big,cancel it and return the payment ticket form again (with an additional error message)

                    // cancel the payment ticket
                    stringResponse = httpService.manageTransact(configuration, partnerTransactionId, response.getId(), TransactionType.CANCEL_TRANSACTION);
                    paymentResponse = handleSetAnnulTransacResponse(stringResponse, partnerTransactionId, request);
                    break;
            }
        }


        return paymentResponse;
    }


    /**
     * Create the right PaymentResponse from the setFinTransac response
     *
     * @param stringResponse setFinTransac response
     * @param numTransac     the partnerTransactionId
     * @return
     */
    private PaymentResponse handleSetFinTransacResponse(String stringResponse, String numTransac) {
        PaymentResponse paymentResponse;

        SetFinTransac response = SetFinTransac.fromXml(stringResponse);

        if (response == null || response.getCodeErreur() == null) {
            paymentResponse = this.responseFailure(APIResponseError.fromXml(stringResponse));
        } else {
            paymentResponse = PaymentResponseSuccess.PaymentResponseSuccessBuilder.aPaymentResponseSuccess()
                    .withPartnerTransactionId(numTransac)
                    .withTransactionDetails(new EmptyTransactionDetails())
                    .withStatusCode(response.getCodeErreur())
                    .build();
        }
        return paymentResponse;
    }


    private PaymentResponse handleSetAnnulTransacResponse(String stringResponse, String numTransac, PaymentRequest request) {
        PaymentResponse paymentResponse;

        if ("true".equalsIgnoreCase(stringResponse)) {
            // if cancellation is OK, return a new form to enter a new payment ticket

            // create the form
            PaymentFormConfigurationResponse paymentFormConfigurationResponse = createRetryForm(request.getLocale(), request.getAmount());

            // create RequestContext for the next step (STEP2 again)
            Map<String, String> requestContextMap = new HashMap<>();
            requestContextMap.put(RequestContextKeys.CONTEXT_DATA_STEP, STEP2);
            requestContextMap.put(RequestContextKeys.NUMTRANSAC, numTransac);

            RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext()
                    .withRequestData(requestContextMap)
                    .build();

            // return the form to ask for a new payment ticket
            paymentResponse = PaymentResponseFormUpdated.PaymentResponseFormUpdatedBuilder.aPaymentResponseFormUpdated()
                    .withPaymentFormConfigurationResponse(paymentFormConfigurationResponse)
                    .withRequestContext(requestContext)
                    .build();


        } else if ("false".equalsIgnoreCase(stringResponse)) {
            // if cancellation is NOT ok, return a failure response
            String errorMessage = "GlobalPos API is unable to cancel the ticket";
            LOGGER.info(errorMessage);
            paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder.aPaymentResponseFailure()
                    .withPartnerTransactionId(numTransac)
                    .withFailureCause(FailureCause.INVALID_DATA)
                    .withErrorCode(errorMessage)
                    .build();

        } else {
            paymentResponse = this.responseFailure(APIResponseError.fromXml(stringResponse));

        }

        return paymentResponse;
    }


    /**
     * Create the form for the end of the step1
     * The shopper will be able to enter his check
     *
     * @param locale to know the language
     * @return PaymentFormConfigurationResponseSpecific
     */
    private PaymentFormConfigurationResponseSpecific createRetryForm(Locale locale, Amount amount) {
        if (locale == null) {
            throw new InvalidDataException("locale must not be null when creating the RETRY payment ticket form");
        }

        if (amount == null || amount.getAmountInSmallestUnit() == null || amount.getCurrency() == null) {
            throw new InvalidDataException("amount must not be null when creating the RETRY payment ticket form");
        }
        List<PaymentFormField> listForm = new ArrayList<>();

        // create a field text to display why the buyer has to retry
        String sAmount = AmountParse.splitDecimal(amount).toString();
        String sCurrency = amount.getCurrency().getSymbol(locale);
        PaymentFormDisplayFieldText displayRetryMessage = PaymentFormDisplayFieldText.PaymentFormDisplayFieldTextBuilder.aPaymentFormDisplayFieldText()
                .withContent(i18n.getMessage("formCabTitre.retryMessage", locale, sAmount, sCurrency))
                .build();
        listForm.add(displayRetryMessage);

        listForm.add(createInputFieldText(locale));

        CustomForm customForm = CustomForm.builder()
                .withCustomFields(listForm)
                .withDescription(i18n.getMessage("customFormTitre.description", locale))
                .build();

        return PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                .aPaymentFormConfigurationResponseSpecific()
                .withPaymentForm(customForm)
                .build();
    }


    private PaymentFormConfigurationResponseSpecific createSimpleForm(Locale locale) {
        if (locale == null) {
            throw new InvalidDataException("locale must not be null when creating the payment ticket form");
        }
        List<PaymentFormField> listForm = new ArrayList<>();
        listForm.add(createInputFieldText(locale));

        CustomForm customForm = CustomForm.builder()
                .withCustomFields(listForm)
                .withDescription(i18n.getMessage("customFormTitre.description", locale))
                .build();

        return PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                .aPaymentFormConfigurationResponseSpecific()
                .withPaymentForm(customForm)
                .build();
    }

    private PaymentFormInputFieldText createInputFieldText(Locale locale) {
        // Pattern: regexp for exactly 44 numbers
        return PaymentFormInputFieldText.PaymentFormFieldTextBuilder.aPaymentFormFieldText()
                .withRequiredErrorMessage(i18n.getMessage("formCabTitre.requiredErrorMessage", locale))
                .withValidation(Pattern.compile("^[0-9]{44}$"))
                .withValidationErrorMessage(i18n.getMessage("formCabTitre.validationErrorMessage", locale))
                .withPlaceholder("")
                .withKey(FormConfigurationKeys.CABTITRE)
                .withLabel(i18n.getMessage("formCabTitre.label", locale))
                .withRequired(true)
                .withInputType(InputType.NUMBER)
                .withFieldIcon(FieldIcon.CARD)
                .build();
    }


    /**
     * Create the ResponseFailure
     *
     * @param response the responseError return by the API GlobalPOS
     * @return PaymentResponseFailure
     */
    public PaymentResponseFailure responseFailure(APIResponseError response) {

        LOGGER.info("Failure While calling API:{}", response);
        return PaymentResponseFailure.PaymentResponseFailureBuilder.aPaymentResponseFailure()
                .withErrorCode(PluginUtils.truncate(response.getMessage(), 50))
                .withFailureCause(PluginUtils.getFailureCause(response.getError()))
                .build();
    }
}
