package com.payline.payment.globalpos.bean.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.payline.payment.globalpos.exception.InvalidDataException;
import lombok.Getter;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIResponseError {
    @Getter
    @JsonProperty("http_status")
    private String httpStatus;
    @Getter
    private int error;
    @Getter
    private String message;
    @Getter
    private String detail;
    private static XmlMapper xmlMapper = new XmlMapper();

    public static APIResponseError fromXml(String xml) {
        try {
            return xmlMapper.readValue(xml, APIResponseError.class);
        } catch (IOException e) {
            throw new InvalidDataException("Unable to parse XML GlobalPOSAPIResponse", e);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
