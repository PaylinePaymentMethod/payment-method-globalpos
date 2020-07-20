package com.payline.payment.globalpos.service;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.exception.InvalidDataException;
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
    private final String guid = "123";
    private final String storeCode = "456";
    private final String checkoutNumber = "789";

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
        Assertions.assertEquals(response, stringResponse.getContent());
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
        Assertions.assertEquals(response, stringResponse.getContent());
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

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, null, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID()));

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

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, guid, null, checkoutNumber, MockUtils.getTRANSACTIONID()));

        Assertions.assertEquals("CODEMAGASIN is missing", thrown.getMessage());
    }

    @Test
    void getTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.getTransacKO()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.getTransact(configuration, guid, storeCode, checkoutNumber, MockUtils.getTRANSACTIONID()));

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

        Assertions.assertEquals(response, stringResponse.getContent());

    }

    @Test
    void getTitreDetailTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.getTitreTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.manageTransact(configuration, MockUtils.getNumTransac(), MockUtils.getTitre(), TransactionType.DETAIL_TRANSACTION));

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

        Assertions.assertEquals(response, stringResponse.getContent());

    }

    @Test
    void setFinTransacCode400() {
        StringResponse stringResponse = MockUtils.mockStringResponse(400
                , "OK"
                , MockUtils.setFinTransacOK()
                , null);
        Mockito.doReturn(stringResponse).when(client).get(any(), any());

        Throwable thrown = assertThrows(InvalidDataException.class,
                () -> httpService.manageTransact(configuration, MockUtils.getNumTransac(), PaymentServiceImpl.STATUS.COMMIT.name(), TransactionType.FINALISE_TRANSACTION));

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
}