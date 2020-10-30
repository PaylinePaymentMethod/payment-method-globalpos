package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.constant.FormConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.RequestContextKeys;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.PaymentFormContext;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.bean.paymentform.bean.field.InputType;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormDisplayFieldText;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormInputFieldText;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PaymentServiceImplTest {
    private String cabTitre = "321123";

    @InjectMocks
    @Spy
    PaymentServiceImpl service = new PaymentServiceImpl();

    @Mock
    private HttpService httpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private PaymentRequest createFullRequest() {
        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, cabTitre);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        return MockUtils.aPaylinePaymentRequestBuilder().withPaymentFormContext(paymentFormContext).build();
    }


    @Test
    void paymentRequestNominal() {
        // create request
        PaymentRequest request = createFullRequest();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn(MockUtils.setFinTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseSuccess.class, response.getClass());
        PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) response;
        Assertions.assertEquals("5e7db72846ebd", responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("1", responseSuccess.getStatusCode());
        Assertions.assertNull(responseSuccess.getReservedAmount());
        Assertions.assertEquals(EmptyTransactionDetails.class, responseSuccess.getTransactionDetails().getClass());
    }

    @Test
    void paymentRequestNoCabTitre() {
        // create request
        PaymentRequest request = MockUtils.aPaylinePaymentRequest();

        // init mocks
        Mockito.verify(httpService, Mockito.never()).getTransact(any(), any(), any(), any(), any());
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), any());

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;

        Assertions.assertEquals("issues with the PaymentFormContext", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
        Assertions.assertNull(responseFailure.getPartnerTransactionId());
    }

    @Test
    void paymentRequestGetTransactFailure() {
        // create request
        PaymentRequest request = createFullRequest();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacKO()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), any());

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;

        Assertions.assertEquals("Magasin inconnu de l'enseigne", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
        Assertions.assertNull(responseFailure.getPartnerTransactionId());
    }

    @Test
    void paymentRequestGetTitreDetailFailure() {
        // create request
        PaymentRequest request = createFullRequest();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacKO()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;

        Assertions.assertEquals("La transaction actuelle n'existe pas", responseFailure.getErrorCode());
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
        Assertions.assertEquals("5e7db72846ebd", responseFailure.getPartnerTransactionId());
    }

    @Test
    void paymentRequestTicketTooBig() {
        // create request
        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, cabTitre);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(500))
                .withLocale(Locale.FRANCE)

                .withPaymentFormContext(paymentFormContext)
                .build();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn("<xml><codeErreur>1</codeErreur></xml>").when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseFormUpdated.class, response.getClass());
        PaymentResponseFormUpdated responseFormUpdated = (PaymentResponseFormUpdated) response;

        Assertions.assertFalse(responseFormUpdated.getRequestContext().getRequestData().isEmpty());
        Assertions.assertEquals("5e7db72846ebd", responseFormUpdated.getRequestContext().getRequestData().get(RequestContextKeys.NUMTRANSAC));
        Assertions.assertEquals(RequestContextKeys.STEP_RETRY, responseFormUpdated.getRequestContext().getRequestData().get(RequestContextKeys.CONTEXT_DATA_STEP));

        Assertions.assertNotNull(responseFormUpdated.getPaymentFormConfigurationResponse());
        Assertions.assertEquals(PaymentFormConfigurationResponseSpecific.class, responseFormUpdated.getPaymentFormConfigurationResponse().getClass());
        PaymentFormConfigurationResponseSpecific formConfigurationResponseSpecific = (PaymentFormConfigurationResponseSpecific) responseFormUpdated.getPaymentFormConfigurationResponse();

        Assertions.assertNotNull(formConfigurationResponseSpecific.getPaymentForm());
        Assertions.assertEquals(CustomForm.class, formConfigurationResponseSpecific.getPaymentForm().getClass());
        CustomForm customForm = (CustomForm) formConfigurationResponseSpecific.getPaymentForm();

        Assertions.assertNotNull(customForm.getCustomFields());
        Assertions.assertEquals(2, customForm.getCustomFields().size());

        Assertions.assertEquals(PaymentFormDisplayFieldText.class, customForm.getCustomFields().get(0).getClass());
        Assertions.assertEquals("Vous ne pouvez pas utiliser ce bon pour une commande inférieure a 10 €", ((PaymentFormDisplayFieldText) customForm.getCustomFields().get(0)).getContent());
        Assertions.assertEquals(PaymentFormInputFieldText.class, customForm.getCustomFields().get(1).getClass());
        Assertions.assertEquals("Numéro de bon d'achat incorrect", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getValidationErrorMessage());
        Assertions.assertEquals("Entrez le code barre du titre", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getLabel());
        Assertions.assertEquals("Numéro de bon d'achat incorrect", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getRequiredErrorMessage());
        Assertions.assertEquals("cabTitre", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getKey());
        Assertions.assertEquals("Numéro de bon d'achat", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getPlaceholder());
        Assertions.assertEquals(InputType.NUMBER, ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getInputType());
        Assertions.assertFalse(((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).isDisabled());
    }

    @Test
    void paymentRequestTicketTooBigCancellationKO() {
        // create request
        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, cabTitre);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(500))
                .withPaymentFormContext(paymentFormContext)
                .build();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn("<xml><http_status>200</http_status><error>-31</error><message>Erreur d'annulation du titre en m�moire</message><detail/></xml>").when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());

        Assertions.assertEquals("Erreur d'annulation du titre en m�moire", ((PaymentResponseFailure) response).getErrorCode());
        Assertions.assertEquals(MockUtils.getNumTransac(), ((PaymentResponseFailure) response).getPartnerTransactionId());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionDetails());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionAdditionalData());
        Assertions.assertNull(((PaymentResponseFailure) response).getWallet());
    }


    @Test
    void paymentRequestTicketTooSmall() {
        // create request
        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, cabTitre);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(50000))
                .withPaymentFormContext(paymentFormContext)
                .build();

        // init mocks
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn(MockUtils.setFinTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseSuccess.class, response.getClass());
        PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) response;
        Assertions.assertEquals("5e7db72846ebd", responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("1", responseSuccess.getStatusCode());
        Assertions.assertNotNull(responseSuccess.getReservedAmount());
        Assertions.assertEquals("1000", responseSuccess.getReservedAmount().getAmountInSmallestUnit().toString());
        Assertions.assertEquals(EmptyTransactionDetails.class, responseSuccess.getTransactionDetails().getClass());
    }


    @Test
    void paymentRequestRetry() {
        // create request
        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, cabTitre);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        Map<String, String> requestContextParameters = new HashMap<>();
        requestContextParameters.put(RequestContextKeys.CONTEXT_DATA_STEP, RequestContextKeys.STEP_RETRY);
        requestContextParameters.put(RequestContextKeys.NUMTRANSAC, "5e7db72846ebd");

        RequestContext requestContext = RequestContext.RequestContextBuilder
                .aRequestContext()
                .withRequestData(requestContextParameters)
                .withSensitiveRequestData(new HashMap<>())
                .build();

        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withPaymentFormContext(paymentFormContext)
                .withRequestContext(requestContext)
                .build();


        // create Mock
        Mockito.verify(httpService, Mockito.never()).getTransact(any(), any(), any(), any(), any());
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn(MockUtils.setFinTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));
        Mockito.verify(httpService, Mockito.never()).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));

        // call method
        PaymentResponse response = service.paymentRequest(request);

        // assertions
        Assertions.assertEquals(PaymentResponseSuccess.class, response.getClass());
        PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) response;
        Assertions.assertEquals("5e7db72846ebd", responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("1", responseSuccess.getStatusCode());
        Assertions.assertNull(responseSuccess.getReservedAmount());
        Assertions.assertEquals(EmptyTransactionDetails.class, responseSuccess.getTransactionDetails().getClass());
    }

    @Test
    void askForAddingTicketWithNullParameter() {

        Map<String, String> paymentFormParameters = new HashMap<>();
        paymentFormParameters.put(FormConfigurationKeys.CABTITRE, null);

        PaymentFormContext paymentFormContext = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameters)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();

        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder().withPaymentFormContext(paymentFormContext).build();

        String partnerTransactionId = "5e7db72846ebd";

        // assertions
        Assertions.assertThrows(InvalidDataException.class, () -> service.askForAddingTicket(paymentRequest, partnerTransactionId));
    }


}