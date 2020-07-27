package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.response.GetTitreDetailTransac;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.bean.paymentform.bean.field.FieldIcon;
import com.payline.pmapi.bean.paymentform.bean.field.InputType;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormDisplayFieldText;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormInputFieldText;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import static com.payline.payment.globalpos.utils.constant.RequestContextKeys.STEP2;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PaymentServiceImplTest {

    @InjectMocks
    @Spy
    PaymentServiceImpl service = new PaymentServiceImpl();

    @Mock
    private HttpService httpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void step1OK() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequest();
        Mockito.doReturn(MockUtils.getTransacOK()).when(httpService).getTransact(any(), any(), any(), any(), any());
        PaymentResponse response = service.step1(request);

        Assertions.assertEquals(PaymentResponseFormUpdated.class, response.getClass());
        Assertions.assertEquals("Entrez votre titre", ((PaymentFormConfigurationResponseSpecific) ((PaymentResponseFormUpdated) response).getPaymentFormConfigurationResponse()).getPaymentForm().getDescription());
    }

    @Test
    void step1KO() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequest();
        Mockito.doReturn(MockUtils.getTransacKO()).when(httpService).getTransact(any(), any(), any(), any(), any());
        PaymentResponse response = service.step1(request);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        Assertions.assertEquals("Magasin inconnu de l'enseigne", ((PaymentResponseFailure) response).getErrorCode());
    }

    @Test
    void step1KOError60() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequest();
        Mockito.doReturn(MockUtils.getTransacKO60()).when(httpService).getTransact(any(), any(), any(), any(), any());
        PaymentResponse response = service.step1(request);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        Assertions.assertEquals("step1KOError60", ((PaymentResponseFailure) response).getErrorCode());
    }

    @Test
    void step1KOError30() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequest();
        Mockito.doReturn(MockUtils.getTransacKO30()).when(httpService).getTransact(any(), any(), any(), any(), any());
        PaymentResponse response = service.step1(request);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        Assertions.assertEquals("step1KOError30", ((PaymentResponseFailure) response).getErrorCode());
    }

    @Test
    void step2NoCabTitre() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, null).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(null))
                .build();

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> service.step2(request));

        Assertions.assertEquals("issues with the PaymentFormContext", thrown.getMessage());
    }

    @Test
    void step2CheckEqual() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(new Amount(new BigInteger(MockUtils.getAmountValue()), Currency.getInstance("EUR")))
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();
        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));

        Mockito.doReturn(MockUtils.setFinTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.FINALISE_TRANSACTION));

        PaymentResponse response = service.step2(request);

        Assertions.assertEquals(PaymentResponseSuccess.class, response.getClass());

        Assertions.assertEquals("5e7db72846ebd", ((PaymentResponseSuccess) response).getPartnerTransactionId());
        Assertions.assertEquals("1", ((PaymentResponseSuccess) response).getStatusCode());
    }

    @Test
    void step2CheckBigger() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(500))
                .withLocale(Locale.FRANCE)
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();

        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        Mockito.doReturn("true").when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));
        PaymentResponse response = service.step2(request);

        Assertions.assertEquals(PaymentResponseFormUpdated.class, response.getClass());
        PaymentResponseFormUpdated responseFormUpdated = (PaymentResponseFormUpdated) response;

        Assertions.assertNotNull(responseFormUpdated.getPaymentFormConfigurationResponse());
        Assertions.assertEquals(PaymentFormConfigurationResponseSpecific.class, responseFormUpdated.getPaymentFormConfigurationResponse().getClass());
        PaymentFormConfigurationResponseSpecific formConfigurationResponseSpecific = (PaymentFormConfigurationResponseSpecific) responseFormUpdated.getPaymentFormConfigurationResponse();

        Assertions.assertNotNull(formConfigurationResponseSpecific.getPaymentForm());
        Assertions.assertEquals(CustomForm.class, formConfigurationResponseSpecific.getPaymentForm().getClass());
        CustomForm customForm = (CustomForm) formConfigurationResponseSpecific.getPaymentForm();

        Assertions.assertNotNull(customForm.getCustomFields());
        Assertions.assertEquals(2, customForm.getCustomFields().size());

        Assertions.assertEquals(PaymentFormDisplayFieldText.class, customForm.getCustomFields().get(0).getClass());
        Assertions.assertEquals("Vous ne pouvez pas utiliser ce bon pour une commande inférieure a 5.00 €", ((PaymentFormDisplayFieldText) customForm.getCustomFields().get(0)).getContent());

        Assertions.assertEquals(PaymentFormInputFieldText.class, customForm.getCustomFields().get(1).getClass());
        Assertions.assertEquals("Format de code barre incorrect", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getValidationErrorMessage());
        Assertions.assertEquals("Entrez le code barre du titre", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getLabel());
        Assertions.assertEquals("Code barre incorrect", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getRequiredErrorMessage());
        Assertions.assertEquals("cabTitre", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getKey());
        Assertions.assertEquals("", ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getPlaceholder());
        Assertions.assertEquals(InputType.NUMBER, ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getInputType());
        Assertions.assertEquals(FieldIcon.CARD, ((PaymentFormInputFieldText) customForm.getCustomFields().get(1)).getFieldIcon());

    }

    @Test
    void step2CheckBiggerAndCancelKO() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(500))
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();

        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));

        Mockito.doReturn("false").when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.CANCEL_TRANSACTION));
        PaymentResponse response = service.step2(request);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());

        Assertions.assertEquals("GlobalPos API is unable to cancel the ticket", ((PaymentResponseFailure) response).getErrorCode());
        Assertions.assertEquals(MockUtils.getNumTransac(), ((PaymentResponseFailure) response).getPartnerTransactionId());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionDetails());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionAdditionalData());
        Assertions.assertNull(((PaymentResponseFailure) response).getWallet());

    }

    @Test
    void step2checkLower() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(1500))
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();

        Mockito.doReturn(MockUtils.getTitreTransacOK()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));

        PaymentResponse response = service.step2(request);

        Assertions.assertEquals(PaymentResponseSuccess.class, response.getClass());
        PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) response;

        Assertions.assertEquals("1", responseSuccess.getStatusCode());
        Assertions.assertEquals(EmptyTransactionDetails.class, responseSuccess.getTransactionDetails().getClass());
        Assertions.assertEquals(MockUtils.getNumTransac(), responseSuccess.getPartnerTransactionId());
        Assertions.assertEquals("1000", responseSuccess.getReservedAmount().getAmountInSmallestUnit().toString());

    }

    @Test
    void step2KO() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withAmount(MockUtils.aPaylineAmount(1500))
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();

        Mockito.doReturn(MockUtils.getTitreTransacKO()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));
        PaymentResponse response = service.step2(request);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());

        Assertions.assertEquals("La transaction actuelle n'existe pas", ((PaymentResponseFailure) response).getErrorCode());
        Assertions.assertNull(((PaymentResponseFailure) response).getPartnerTransactionId());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionDetails());
        Assertions.assertNull(((PaymentResponseFailure) response).getTransactionAdditionalData());
        Assertions.assertNull(((PaymentResponseFailure) response).getWallet());

    }

    @Test
    void step2KOWrongAmount() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2(MockUtils.getTitre()))
                .build();

        Mockito.doReturn(MockUtils.getTitreTransacWrongAmount()).when(httpService).manageTransact(any(), any(), any(), eq(TransactionType.DETAIL_TRANSACTION));

        assertThrows(NumberFormatException.class, () -> service.step2(request));

    }

    @Test
    void PSStep1() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder().build();

        // Mock the method to be called but don't bother with the result
        Mockito.doReturn(null).when(service).step1(any());
        service.paymentRequest(request);
        Mockito.verify(service, Mockito.times(1)).step1(request);
    }

    @Test
    void PSStep2() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withRequestContext(MockUtils.aRequestContextBuilderStep(STEP2, MockUtils.getNumTransac()).build())
                .withPaymentFormContext(MockUtils.aPaymentFormContextStep2("123456789"))
                .build();

        // Mock the method to be called but don't bother with the result
        Mockito.doReturn(null).when(service).step2(any());
        service.paymentRequest(request);
        Mockito.verify(service, Mockito.times(1)).step2(request);
    }


    @Test
    void PSStepFalse() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder()
                .withPaymentFormContext(MockUtils.aPaymentFormContext())
                .withRequestContext(MockUtils.aRequestContextBuilderStep("STEP", MockUtils.getNumTransac()).build())
                .build();
        PaymentResponse response = service.paymentRequest(request);
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
    }

    @Test
    void PSStepPluginException() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder().build();

        // Mock the method to be called but don't bother with the result
        FailureCause cause = FailureCause.INVALID_DATA;
        String errorMessage = "foo";
        Mockito.doThrow(new PluginException(errorMessage, cause)).when(service).step1(any());
        PaymentResponse response = service.paymentRequest(request);
        Mockito.verify(service, Mockito.times(1)).step1(request);
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;
        Assertions.assertEquals(cause, responseFailure.getFailureCause());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
    }

    @Test
    void PSStepRuntimeException() {
        PaymentRequest request = MockUtils.aPaylinePaymentRequestBuilder().build();

        // Mock the method to be called but don't bother with the result
        FailureCause cause = FailureCause.INTERNAL_ERROR;
        String errorMessage = "plugin error: NullPointerException";
        Mockito.doThrow(new NullPointerException()).when(service).step1(any());
        PaymentResponse response = service.paymentRequest(request);
        Mockito.verify(service, Mockito.times(1)).step1(request);
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;
        Assertions.assertEquals(cause, responseFailure.getFailureCause());
        Assertions.assertEquals(errorMessage, responseFailure.getErrorCode());
    }

    @Test
    void date() throws ParseException {
        final String dateFormat = "dd/MM/yyyy";
        LocalDateTime localDateTime = LocalDateTime.now();
        Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        GetTitreDetailTransac response = GetTitreDetailTransac.fromXml(MockUtils.getTitreTransacOK());
        Date checkDate = new SimpleDateFormat(dateFormat).parse(response.getDateValid());

        Assertions.assertEquals(1, checkDate.compareTo(date));
    }
}