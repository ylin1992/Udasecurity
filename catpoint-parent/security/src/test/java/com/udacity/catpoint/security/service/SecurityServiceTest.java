package com.udacity.catpoint.security.service;

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

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService fakeImageService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, fakeImageService);
    }

    // Test 1
    @Test
    void alarmIsArmedAndSensorIsActivated_putSystemIntoPending() {
        // for details, see Sensor's constructor
        Sensor sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);

        // force to go to NO_ALARM case in handleSensorActivated()
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityService, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

}
