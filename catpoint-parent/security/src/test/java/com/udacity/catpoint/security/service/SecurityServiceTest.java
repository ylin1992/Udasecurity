package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;


    @BeforeEach
    void init() {
        imageService = new FakeImageService();
        securityService = new SecurityService(securityRepository, imageService);
        // for details, see Sensor's constructor
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW);
    }

    // Test 1
    @Test
    void alarmIsArmedAndSensorIsActivated_putSystemIntoPending() {

        // force it to go to NO_ALARM case in handleSensorActivated()
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityService, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void alarmIsArmedAndSensorIsActivatedAndSystemIsPending_setAlarmToAlarmStatus() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
