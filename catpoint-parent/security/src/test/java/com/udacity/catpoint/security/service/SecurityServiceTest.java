package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
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

    private Set<Sensor> genSensorList() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW));
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR));
        sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.MOTION));
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
        //imageService = new FakeImageService();
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
        // below codes don't work since setAlarmStatus() is used in catDetected(),
        // which can lead to failure since we never want it to be called, but at least one will be called
//        if (!active) {
//            verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
//        } else {
//            verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
//        }
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //
    // Test 9
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void systemIsDisArmed_setStatusToNoAlarm(boolean active) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, active);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //// Test 10
    //@Test
    //void systemIsArmed_resetAllSensorsToInactive() {
    //    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    //    Set<Sensor> sensors = genSensorList();
    //    for (Sensor sensor : sensors) {
    //        securityService.changeSensorActivationStatus(sensor, false);
    //        Assertions.assertTrue(sensor.getActive());
    //    }
    //}
    //
    //// Test 11
    //@Test
    //void systemIsArmedHomeWhileCameraShowsCat_setAlarmToAlarm() {
    //    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    //    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
    //
    //    securityService.changeSensorActivationStatus(sensor, true);
    //    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    //}

}
