package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private Sensor sensor;

    private Set<Sensor> genSensorSet() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW));
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR));
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.MOTION));

        for (Sensor sensor : sensors) {
            sensor.setActive(true);
        }
        return sensors;
    }

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private SecurityService securityService;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void init() {
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
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @Test
    void alarmIsArmedAndSensorIsActivatedAndSystemIsPending_setAlarmToAlarmStatus() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 3
    @Test
    void pendingAlarmAndSensorsAreInactive_returnToNoAlarmState() {
        // handleSensorDeactivated, case PENDING_ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alarmIsActive_changeInSensorStateDoesNotAffectAlarmState(boolean active) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, active);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5
    @Test
    void sensorIsActivatedAndSystemIsPending_changeToAlarmState() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"PENDING_ALARM", "NO_ALARM", "ALARM"})
    void sensorIsDeactivatedWhileInactive_noChangeInAlarmState(AlarmStatus alarmStatus) {
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //
    // Test 7
    // test target: processImage()
    @Test
    void imageServiceFindsACatWhenSystemIsArmedHome_putSystemIntoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        // force to detect a cat
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //
    // Test 8
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void imageServiceIdentifiesAnImageButNotACat_changeStatusToNoAlarmIfTheSensorsAreNotActive(boolean active) {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        sensor.setActive(active);
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //
    // Test 9
    @Test
    void systemIsDisArmed_setStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void systemIsArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        // initialize a list of active sensors
        Set<Sensor> dummySensors = genSensorSet();
        when(securityRepository.getSensors()).thenReturn(dummySensors);
        securityService.setArmingStatus(armingStatus);
        dummySensors.forEach(s -> {
            Assertions.assertFalse(s.getActive());
        });
    }

    //// Test 11
    @Test
    void systemIsArmedHomeWhileCameraShowsCat_setAlarmToAlarm() {
        BufferedImage dummyCatImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(dummyCatImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        //Assertions.assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }


    // additional test for Test 8
    @Test
    void systemIsAlarmSensorIsArmedCatIsDetected_imageDoesNotContainCat_systemKeepsAlarmState() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getSensorsState()).thenReturn(true);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
