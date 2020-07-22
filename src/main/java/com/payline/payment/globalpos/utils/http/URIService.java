package com.payline.payment.globalpos.utils.http;

import com.payline.payment.globalpos.exception.InvalidDataException;
import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URIService {
    private static final String BASE_PATH = "wstransac";
    private static final String GET_TRANSACT_PATH = "gettransac";
    private static final String GET_TITRE_DETAIL_TRANSACT_PATH = "gettitredetailtransac";
    private static final String SET_ANNUL_TITRE_TRANSACT_PATH = "setannultitretransac";
    private static final String SET_FIN_TRANSACT_PATH = "setfintransac";

    private static final String GUID = "guid";
    private static final String MAG_CAISSE = "magcaisse";
    private static final String DATE_TICKET = "dateticket";
    private static final String NUM_TICKET = "numticket";
    private static final String NUM_TRANSAC = "numtransac";
    private static final String ID = "id";
    private static final String CAB_TITRE = "cabtitre";
    private static final String STATUS = "statut";

    // refund paths
    private static final String INDEX = "index.php";
    private static final String REFUND_BASE_PATH = "Wscardb2c";
    private static final String GET_AUTH_TOKEN = "getAuthToken";
    private static final String SET_CREATE_CARD = "setCreatCard";
    private static final String SET_GEN_CARD_MAIL = "setGenCardMail";

    // refund parameters
    private static final String NUM_CARD = "numcarte";
    private static final String FLAG_MAIL = "flagmail";

    private URIService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Create the URL with all values needed to create a transaction
     *
     * @param baseUrl
     * @param guid
     * @param storeId
     * @param dateTicket
     * @param numTicket
     * @return
     */
    public static URI createGetTransactionURL(String baseUrl, String guid, String storeId, String dateTicket, String numTicket) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(BASE_PATH, GET_TRANSACT_PATH);
            builder.addParameter(GUID, guid);
            builder.addParameter(MAG_CAISSE, storeId);
            builder.addParameter(DATE_TICKET, dateTicket);
            builder.addParameter(NUM_TICKET, numTicket);
            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call getTransaction", e);
        }

    }

    /**
     * Create the URL with all values needed to add a payment ticket to the transaction
     *
     * @param baseUrl
     * @param guid
     * @param numTransac
     * @param cabTitre
     * @return
     */
    public static URI createGetTitreDetailTransactionURL(String baseUrl, String guid, String numTransac, String cabTitre) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(BASE_PATH, GET_TITRE_DETAIL_TRANSACT_PATH);
            builder.addParameter(GUID, guid);
            builder.addParameter(NUM_TRANSAC, numTransac);
            builder.addParameter(CAB_TITRE, cabTitre);
            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call getTitreDetailTransaction", e);
        }

    }

    /**
     * @param baseUrl
     * @param guid
     * @param numTransac
     * @param id
     * @return
     */
    public static URI createSetAnnulTitreTransactionURL(String baseUrl, String guid, String numTransac, String id) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(BASE_PATH, SET_ANNUL_TITRE_TRANSACT_PATH);
            builder.addParameter(GUID, guid);
            builder.addParameter(NUM_TRANSAC, numTransac);
            builder.addParameter(ID, id);
            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call setAnnulTitreTransaction", e);
        }

    }

    /**
     * Create the URL with all values needed to end a transaction (finalize or cancel)
     *
     * @param baseUrl
     * @param guid
     * @param numTransac
     * @param status
     * @return
     */
    public static URI createSetFinTransactionURL(String baseUrl, String guid, String numTransac, String status) {
        // create the valid url
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(BASE_PATH, SET_FIN_TRANSACT_PATH);
            builder.addParameter(GUID, guid);
            builder.addParameter(NUM_TRANSAC, numTransac);
            builder.addParameter(STATUS, status);
            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call SetFinTransaction", e);
        }
    }

    /**
     * Create the URL with all values needed to ask for a token
     *
     * @param baseUrl
     * @return
     */
    public static URI createGetAuthTokenURL(String baseUrl) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(INDEX, REFUND_BASE_PATH, GET_AUTH_TOKEN);

            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call getAuthToken", e);
        }
    }

    /**
     * Create the URL with all values needed to ask for a new payment title
     *
     * @param baseUrl
     * @return
     */
    public static URI createSetCreateCardURL(String baseUrl) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(INDEX, REFUND_BASE_PATH, SET_CREATE_CARD);

            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call setCreatCard", e);
        }
    }

    /**
     * Create the URL with all values needed to validate the new payment title creation
     *
     * @param baseUrl
     * @param cardId
     * @param flagmail
     * @return
     */
    public static URI createSetGenCardMailURL(String baseUrl, String cardId, String flagmail) {
        try {
            URIBuilder builder = new URIBuilder(baseUrl);
            builder.setPathSegments(INDEX, REFUND_BASE_PATH, SET_GEN_CARD_MAIL);

            builder.addParameter(NUM_CARD, cardId);
            builder.addParameter(FLAG_MAIL, flagmail);


            return builder.build().toURL().toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new InvalidDataException("Unable to create URL to call setGenMail", e);
        }
    }

}
