package com.payline.payment.globalpos.bean.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.payline.payment.globalpos.exception.InvalidDataException;
import lombok.Getter;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetFinTransac {
    @Getter
    private String codeErreur;
    private static XmlMapper xmlMapper = new XmlMapper();

    public static SetFinTransac fromXml(String xml) {
        try {
            return xmlMapper.readValue(xml, SetFinTransac.class);
        } catch (IOException e) {
            throw new InvalidDataException("Unable to parse XML SetFinTransac", e);
        }
    }
}
