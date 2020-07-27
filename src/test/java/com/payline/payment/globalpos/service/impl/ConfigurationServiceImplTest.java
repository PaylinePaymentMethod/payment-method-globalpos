package com.payline.payment.globalpos.service.impl;

import com.payline.payment.globalpos.MockUtils;
import com.payline.payment.globalpos.bean.configuration.RequestConfiguration;
import com.payline.payment.globalpos.service.HttpService;
import com.payline.payment.globalpos.utils.properties.ReleaseProperties;
import com.payline.pmapi.bean.configuration.ReleaseInformation;
import com.payline.pmapi.bean.configuration.parameter.AbstractParameter;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class ConfigurationServiceImplTest {

    @InjectMocks
    ConfigurationServiceImpl configurationServiceImpl = new ConfigurationServiceImpl();
    @Mock
    private ReleaseProperties releaseProperties;
    Locale locale = Locale.getDefault();

    @Mock
    private HttpService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    void getParameters() {
        List<AbstractParameter> parameters = configurationServiceImpl.getParameters(Locale.FRANCE);

        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(5, parameters.size());

        for (AbstractParameter p : parameters) {
            Assertions.assertNotNull(p.getKey());

            // assert that the message exists (Only french Locale)
            Assertions.assertNotNull(p.getLabel());
            // when no message associated to the key, i18n return a message containing ???
            Assertions.assertFalse(p.getLabel().contains("???"));
            Assertions.assertNotNull(p.getDescription());
        }
    }

    @Test
    void check() {
        ContractParametersCheckRequest checkRequest = MockUtils.aContractParametersCheckRequest();
        Mockito.doReturn(MockUtils.getTransacOK()).when(service).getTransact(any(RequestConfiguration.class), any(), any(), any(), any());
        Map<String, String> errors = configurationServiceImpl.check(checkRequest);
        Assertions.assertEquals(0, errors.size());
    }

    @Test
    void getReleaseInformation() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String version = "1.0.0.0";

        // given: the release properties are OK
        doReturn(version).when(releaseProperties).get("release.version");
        Calendar cal = new GregorianCalendar();
        cal.set(2020, Calendar.APRIL, 9);
        doReturn(formatter.format(cal.getTime())).when(releaseProperties).get("release.date");

        // when: calling the method getReleaseInformation
        ReleaseInformation releaseInformation = configurationServiceImpl.getReleaseInformation();

        // then: releaseInformation contains the right values
        assertEquals(version, releaseInformation.getVersion());
        assertEquals(2020, releaseInformation.getDate().getYear());
        assertEquals(Month.APRIL, releaseInformation.getDate().getMonth());
        assertEquals(9, releaseInformation.getDate().getDayOfMonth());
    }

    @Test
    void getName() {
        // when: calling the method getName
        String name = configurationServiceImpl.getName(Locale.getDefault());

        // then: the method returns the name
        assertNotNull(name);
        assertEquals("globalpos", name);
    }
}