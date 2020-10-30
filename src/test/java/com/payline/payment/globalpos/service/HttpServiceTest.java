package com.payline.payment.globalpos.service;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.bean.request.CreateCardBody;
import com.payline.payment.globalpos.bean.request.LoginBody;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.exception.PluginException;
import com.payline.payment.globalpos.service.impl.PaymentServiceImpl;
import com.payline.payment.globalpos.utils.constant.ContractConfigurationKeys;
import com.payline.payment.globalpos.utils.http.HttpClient;
import com.payline.payment.globalpos.utils.http.StringResponse;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.pmapi.bean.payment.ContractProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class HttpServiceTest {
    private static final String guid = "123";
    private static final String storeCode = "456";
    private static final String checkoutNumber = "789";
    private static final String transactionId = MockUtils.getTRANSACTIONID();


    private RequestConfiguration configuration = new RequestConfiguration(
            MockUtils.aContractConfiguration()
            , MockUtils.aPartnerConfiguration()
    );

    @InjectMocks
    private HttpService httpService = HttpService.getInstance();

    @Mock
    private HttpClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    void getTransacOK() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.getTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String response = httpService.getTransact(configuration, guid, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID());

        Assertions.assertDoesNotThrow(() -> httpService.getTransact(configuration, guid, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID()));
        Assertions.assertEquals(stringResponse.getContent(), response);
    }


    @Test
    void getTransacKObutOK() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.getTransacKO()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String response = httpService.getTransact(configuration, guid, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID());

        Assertions.assertDoesNotThrow(() -> httpService.getTransact(configuration, guid, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID()));
        Assertions.assertEquals(stringResponse.getContent(), response);
    }

    @Test
    void getTransacNoGUID() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.getTransacKO()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        Map<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put(ContractConfigurationKeys.CODEMAGASIN, new ContractProperty(MockUtils.getCodeMagasin()));
        contractProperties.put(ContractConfigurationKeys.NUMEROCAISSE, new ContractProperty(MockUtils.getNumeroCaisse()));

        String transactionId = MockUtils.getTRANSACTIONID();

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, null, storeCode, checkoutNumber, transactionId));
      
        Assertions.assertEquals("GUID is missing", thrown.getMessage());
    }

    @Test
    void getTransacNoCodeMagasin() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.getTransacKO()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        Map<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put(ContractConfigurationKeys.GUID, new ContractProperty(MockUtils.getGuid()));
        contractProperties.put(ContractConfigurationKeys.NUMEROCAISSE, new ContractProperty(MockUtils.getNumeroCaisse()));

        String transactionId = MockUtils.getTRANSACTIONID();

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, guid, null, checkoutNumber, transactionId));

        Assertions.assertEquals("CODEMAGASIN is missing", thrown.getMessage());
    }

    @Test
    void getTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.getTransacKO()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String transactionId = MockUtils.getTRANSACTIONID();

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, guid, storeCode, checkoutNumber, transactionId));

        Assertions.assertEquals("GetTransaction wrong data", thrown.getMessage());

    }

    @Test
    void getTitreDetailTransacOK() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.getTitreTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String response = httpService.manageTransact(configuration, MockUtils.getNumTransac(), MockUtils.getTitre(), TransactionType.DETAIL_TRANSACTION);

        Assertions.assertDoesNotThrow(() -> httpService.manageTransact(configuration, MockUtils.getNumTransac(), MockUtils.getTitre(), TransactionType.DETAIL_TRANSACTION));

        Assertions.assertEquals(stringResponse.getContent(), response);

    }

    @Test
    void getTitreDetailTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.getTitreTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());


        String partnerTransactionId = MockUtils.getNumTransac();
        String title = MockUtils.getTitre();
        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.manageTransact(configuration, partnerTransactionId, title, TransactionType.DETAIL_TRANSACTION));

        Assertions.assertEquals("DETAIL_TRANSACTION wrong data", thrown.getMessage());

    }

    @Test
    void setFinTransacOK() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , MockUtils.setFinTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String response = httpService.manageTransact(configuration, MockUtils.getNumTransac(), PaymentServiceImpl.STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION);

        Assertions.assertDoesNotThrow(() -> httpService.manageTransact(configuration, MockUtils.getNumTransac(), PaymentServiceImpl.STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION));

        Assertions.assertEquals(stringResponse.getContent(), response);

    }

    @Test
    void setFinTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.setFinTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String partnerTransactionId = MockUtils.getNumTransac();
        String name =  PaymentServiceImpl.STATUS.COMMIT.name();
        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.manageTransact(configuration, partnerTransactionId, name, TransactionType.FINALISE_TRANSACTION));

        Assertions.assertEquals("FINALISE_TRANSACTION wrong data", thrown.getMessage());
    }

    @Test
    void setAnnulTitreTransactOK() {
        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , "true"
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        String s = httpService.manageTransact(configuration, MockUtils.getNumTransac(), "123", TransactionType.CANCEL_TRANSACTION);

        Assertions.assertNotNull(s);
        Assertions.assertEquals("true", s);
    }

    @Test
    void getAuthToken() {
        // create mock
        String token = "{\n" +
                "    \"error\": 0,\n" +
                "    \"message\": \"\",\n" +
                "    \"token\": \"thisIsAToken\"\n" +
                "}";

        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , token
                , null);
        Mockito.doReturn(stringResponse).when(client).post(any(), any(), any());

        // call method
        LoginBody body = new LoginBody("login", "password", "guid");
        GetAuthToken authToken = httpService.getAuthToken(configuration, body);

        // assertions
        Assertions.assertNotNull(authToken);
        Assertions.assertEquals("thisIsAToken", authToken.getToken());
    }

    @Test
    void getAuthTokenKO() {
        // create mock
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , "foo"
                , null);
        Mockito.doReturn(stringResponse).when(client).post(any(), any(), any());

        // call method
        LoginBody body = new LoginBody("login", "password", "guid");
        Assertions.assertThrows(PluginException.class, () -> httpService.getAuthToken(configuration, body));
    }

    @Test
    void setCreateCard() {
        // create mock
        String card = "{\n" +
                "    \"error\": 0,\n" +
                "    \"message\": \"\",\n" +
                "    \"cartes\": {\n" +
                "        \"cardid\": \"123\",\n" +
                "        \"cardid2\": \"456\",\n" +
                "        \"cardcvv\": \"789\",\n" +
                "        \"montant\": 1000\n" +
                "    }\n" +
                "}";

        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , card
                , null);
        Mockito.doReturn(stringResponse).when(client).post(any(), any(), any());
        // call method
        CreateCardBody body = CreateCardBody.builder()
                .action("foo")
                .dateTransac("foo")
                .email("foo")
                .magCaisse("foo")
                .montant(1)
                .numTransac("foo")
                .typeTitre("foo")
                .build();
        SetCreateCard cardResponse = httpService.setCreateCard(configuration, "token", body);

        //assertions
        Assertions.assertNotNull(cardResponse);
        Assertions.assertNotNull( cardResponse.getCard());
        Assertions.assertEquals( "123", cardResponse.getCard().getCardId());
        Assertions.assertEquals( "456", cardResponse.getCard().getCardId2());
        Assertions.assertEquals( "789", cardResponse.getCard().getCardCvv());
        Assertions.assertEquals( "1000", cardResponse.getCard().getAmount());

    }

    @Test
    void setCreateCardKO() {
        // create mock
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , "foo"
                , null);
        Mockito.doReturn(stringResponse).when(client).post(any(), any(), any());

        // call method
        CreateCardBody body = CreateCardBody.builder()
                .action("foo")
                .dateTransac("foo")
                .email("foo")
                .magCaisse("foo")
                .montant(1)
                .numTransac("foo")
                .typeTitre("foo")
                .build();
        Assertions.assertThrows(PluginException.class, () -> httpService.setCreateCard(configuration, "token", body));
    }

    @Test
    void setGenCardMail() {
        // create mock
        String mail = "{\n" +
                "    \"error\": 0,\n" +
                "    \"message\": \"\"\n" +
                "}";

        StringResponse stringResponse = MockUtils.mockStringResponse(200
                , "OK"
                , mail
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        // call method
        JsonBeanResponse beanResponse = httpService.setGenCardMail(configuration, "token", "foo");

        // assertions
        Assertions.assertNotNull(beanResponse);
        Assertions.assertEquals(0, beanResponse.getError());
    }

    @Test
    void setGenCardMailKO() {
        // create mock
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , "foo"
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        // call method
        Assertions.assertThrows(PluginException.class, () -> httpService.setGenCardMail(configuration, "token", "foo"));

    }
}