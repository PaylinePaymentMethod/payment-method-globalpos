package com.payline.payment.globalpos.utils.constant;

public class ContractConfigurationKeys {
    public static final String GUID = "guid";
    public static final String CODEMAGASIN = "codeMagasin";
    public static final String NUMEROCAISSE = "numeroCaisse";
    public static final String ID = "id";
    public static final String PASSWORD = "password";


    public static final String CB_PROPERTY_LABEL = "contractConfiguration.cb.label";
    public static final String CB_PROPERTY_DESCRIPTION = "contractConfiguration.cb.description";
    public static final String VISA_PROPERTY_LABEL = "contractConfiguration.visa.label";
    public static final String VISA_PROPERTY_DESCRIPTION = "contractConfiguration.visa.description";
    public static final String MASTERCARD_PROPERTY_LABEL = "contractConfiguration.mastercard.label";
    public static final String MASTERCARD_PROPERTY_DESCRIPTION = "contractConfiguration.mastercard.description";
    public static final String AMEX_PROPERTY_LABEL = "contractConfiguration.amex.label";
    public static final String AMEX_PROPERTY_DESCRIPTION = "contractConfiguration.amex.description";

    /* Static utility class : no need to instantiate it (Sonar bug fix) */
    private ContractConfigurationKeys() {
    }
}
