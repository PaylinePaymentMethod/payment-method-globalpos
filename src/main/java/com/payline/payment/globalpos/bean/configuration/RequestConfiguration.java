package com.payline.payment.globalpos.bean.configuration;

import com.payline.payment.globalpos.exception.InvalidDataException;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.request.NotifyTransactionStatusRequest;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.reset.request.ResetRequest;

/**
 * Generic class that supports any type of request's configuration.
 */
public class RequestConfiguration {

    private ContractConfiguration contractConfiguration;
    private PartnerConfiguration partnerConfiguration;

    public RequestConfiguration(ContractConfiguration contractConfiguration, PartnerConfiguration partnerConfiguration) {
        if (contractConfiguration == null) {
            throw new InvalidDataException("Missing request contractConfiguration");
        }

        if (partnerConfiguration == null) {
            throw new InvalidDataException("Missing request partnerConfiguration");
        }
        this.contractConfiguration = contractConfiguration;
        this.partnerConfiguration = partnerConfiguration;
    }

    public ContractConfiguration getContractConfiguration() {
        return contractConfiguration;
    }

    public PartnerConfiguration getPartnerConfiguration() {
        return partnerConfiguration;
    }

    public static RequestConfiguration build(NotifyTransactionStatusRequest request) {
        return new RequestConfiguration(request.getContractConfiguration(), request.getPartnerConfiguration());
    }

    public static RequestConfiguration build(PaymentRequest request) {
        return new RequestConfiguration(request.getContractConfiguration(), request.getPartnerConfiguration());
    }

    public static RequestConfiguration build(RefundRequest request) {
        return new RequestConfiguration(request.getContractConfiguration(), request.getPartnerConfiguration());
    }

    public static RequestConfiguration build(ResetRequest request) {
        return new RequestConfiguration(request.getContractConfiguration(), request.getPartnerConfiguration());
    }

    public static RequestConfiguration build(ContractParametersCheckRequest request) {
        return new RequestConfiguration(request.getContractConfiguration(), request.getPartnerConfiguration());
    }

}
