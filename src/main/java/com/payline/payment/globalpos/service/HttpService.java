package com.payline.payment.globalpos.service;

import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.bean.request.CreateCardBody;
import com.payline.payment.globalpos.bean.request.LoginBody;
import com.payline.payment.globalpos.bean.response.GetAuthToken;
import com.payline.payment.globalpos.bean.response.JsonBeanResponse;
import com.payline.payment.globalpos.bean.response.SetCreateCard;
import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.payment.globalpos.utils.PluginUtils;
import com.payline.payment.globalpos.utils.constant.ContractConfigurationKeys;
import com.payline.payment.globalpos.utils.constant.PartnerConfigurationKeys;
import com.payline.payment.globalpos.utils.http.HttpClient;
import com.payline.payment.globalpos.utils.http.StringResponse;
import com.payline.payment.globalpos.utils.http.TransactionType;
import com.payline.payment.globalpos.utils.http.URIService;
import com.payline.pmapi.logger.LogManager;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class HttpService {
    private static final Logger LOGGER = LogManager.getLogger(HttpService.class);
    private HttpClient client = HttpClient.getInstance();

    private static final String TOKEN = "token";

    private HttpService() {
    }

    private static class Holder {
        private static final HttpService instance = new HttpService();
    }


    public static HttpService getInstance() {
        return Holder.instance;
    }
    // --- Singleton Holder pattern + initialization END

    protected Header[] initHeaders(String token) {
        return new Header[]{
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader(TOKEN, token)};
    }

    /**
     * Initialize a GlobalPOS transaction
     *
     * @param configuration  the request configuration
     * @param guid           Id of the merchant
     * @param storeCode      Id of the store
     * @param checkoutNumber Sub id of the store
     * @param transactionId  Id of the transaction
     * @return
     */
    public String getTransact(RequestConfiguration configuration, String guid, String storeCode, String checkoutNumber, String transactionId) {
        if (configuration.getPartnerConfiguration() == null) {
            throw new InvalidDataException("PartnerConfiguration is empty");
        }

        if (configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL) == null) {
            throw new InvalidDataException("URL is missing");
        }

        if (PluginUtils.isEmpty(guid)) {
            throw new InvalidDataException("GUID is missing");
        }
        if (PluginUtils.isEmpty(storeCode)) {
            throw new InvalidDataException("CODEMAGASIN is missing");
        }
        String storeId = PluginUtils.createStoreId(storeCode, checkoutNumber);

        // create the valid url
        String baseUrl = configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL);
        URI url = URIService.createGetTransactionURL(baseUrl, guid, storeId, PluginUtils.computeDate(), transactionId);

        // call API
        StringResponse response = client.get(url, null);

        //
        if (!response.isSuccess()) {
            String error = "GetTransaction wrong data";
            LOGGER.error(error, response.getContent());
            throw new InvalidDataException(error);
        }

        return response.getContent();
    }

    public String manageTransact(RequestConfiguration configuration, String numTransact, String urlElement, TransactionType transactionType) {
        verifyData(configuration);

        // create the valid url
        String baseUrl = configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL);
        String guid = configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.GUID).getValue();
        StringResponse response = new StringResponse();
        String error = "";

        switch (transactionType) {
            case FINALISE_TRANSACTION:
                response = client.get(URIService.createSetFinTransactionURL(baseUrl, guid, numTransact, urlElement), null);
                error = "FINALISE_TRANSACTION wrong data";
                break;
            case DETAIL_TRANSACTION:
                response = client.get(URIService.createGetTitreDetailTransactionURL(baseUrl, guid, numTransact, urlElement), null);
                error = "DETAIL_TRANSACTION wrong data";
                break;
            case CANCEL_TRANSACTION:
                response = client.get(URIService.createSetAnnulTitreTransactionURL(baseUrl, guid, numTransact, urlElement), null);
                error = "CANCEL_TRANSACTION wrong data";
                break;
        }

        if (!response.isSuccess()) {
            LOGGER.error(error);
            throw new InvalidDataException(error);
        }

        return response.getContent();
    }


    public GetAuthToken getAuthToken(RequestConfiguration configuration, LoginBody body) {
        verifyData(configuration);

        // create the url
        String baseUrl = configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL);
        URI uri = URIService.createGetAuthTokenURL(baseUrl);
        Header[] headers = new Header[]{new BasicHeader("Content-Type", "application/json")};

        // call API
        StringResponse response = client.post(uri, headers, body.toJson());

        // verify the response
        if (!response.isSuccess()) {
            String errorMessage = "GET_AUTH_TOKEN error";
            LOGGER.error(errorMessage, errorMessage);
            throw new InvalidDataException(errorMessage);
        }
        return GetAuthToken.fromJson(response.getContent());
    }


    public SetCreateCard setCreateCard(RequestConfiguration configuration, String token, CreateCardBody body) {
        verifyData(configuration);

        // create the url
        String baseUrl = configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL);
        URI uri = URIService.createSetCreateCardURL(baseUrl);

        // call API
        StringResponse response = client.post(uri, initHeaders(token), body.toJson());

        // verify the response
        if (!response.isSuccess()) {
            String errorMessage = "SET_CREATE_CARD error";
            LOGGER.error(errorMessage, errorMessage);
            throw new InvalidDataException(errorMessage);
        }

        return SetCreateCard.fromJson(response.getContent());
    }

    public JsonBeanResponse setGenCardMail(RequestConfiguration configuration, String token, String cardId) {
        verifyData(configuration);

        // create the url
        String baseUrl = configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL);
        URI uri = URIService.createSetGenCardMailURL(baseUrl, cardId, "1"); // we always want the API to send an email

        // call API
        StringResponse response = client.get(uri, initHeaders(token));

        // verify response
        if (!response.isSuccess()) {
            String errorMessage = "SET_GEN_CARD_MAIL error";
            LOGGER.error(errorMessage, errorMessage);
            throw new InvalidDataException(errorMessage);
        }

        return JsonBeanResponse.fromJson(response.getContent());
    }


    /**
     * Check if the datas required for all request are empty or not
     *
     * @param configuration the request configuration
     */
    private void verifyData(RequestConfiguration configuration) {
        if (configuration.getPartnerConfiguration() == null) {
            throw new InvalidDataException("PartnerConfiguration is empty");
        }

        if (configuration.getContractConfiguration() == null) {
            throw new InvalidDataException("ContractConfiguration is empty");
        }

        if (configuration.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.URL) == null) {
            throw new InvalidDataException("URL is missing");
        }

        if (configuration.getContractConfiguration().getProperty(ContractConfigurationKeys.GUID) == null) {
            throw new InvalidDataException("GUID is missing");
        }
    }
}
