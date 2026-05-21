package at.qe.skeleton.tests.scheduled;

import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.scheduled.AvailableSensorStationCleaner;
import at.qe.skeleton.services.RaspberryPiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AvailableSensorStationCleanerTest {

    @Mock
    private RaspberryPiService raspberryPiService;

    @InjectMocks
    private AvailableSensorStationCleaner cleaner;

    @Test
    void cleanAvailableSensorStations_removesOnlyAvailableStationsUsingInternalLookup() {
        RaspberryPi raspberryPi = new RaspberryPi();
        SensorStation availableStation = station(11L, DeviceStatus.AVAILABLE);
        SensorStation onlineStation = station(12L, DeviceStatus.ONLINE);
        raspberryPi.setSensorStations(List.of(availableStation, onlineStation));

        Mockito.when(raspberryPiService.getByIdInternal(1L)).thenReturn(raspberryPi);

        cleaner.cleanAvailableSensorStations(1L);

        Mockito.verify(raspberryPiService).getByIdInternal(1L);
        Mockito.verify(raspberryPiService).removeAvailableSensorStationAfterScanTimeOut(1L, 11L);
        Mockito.verify(raspberryPiService, Mockito.never()).getById(1L);
        Mockito.verify(raspberryPiService, Mockito.never()).removeAvailableSensorStationAfterScanTimeOut(1L, 12L);
    }

    @Test
    void cleanAvailableSensorStations_removesAllAvailableStations() {
        RaspberryPi raspberryPi = new RaspberryPi();
        SensorStation firstAvailableStation = station(11L, DeviceStatus.AVAILABLE);
        SensorStation secondAvailableStation = station(12L, DeviceStatus.AVAILABLE);
        raspberryPi.setSensorStations(List.of(firstAvailableStation, secondAvailableStation));

        Mockito.when(raspberryPiService.getByIdInternal(1L)).thenReturn(raspberryPi);

        cleaner.cleanAvailableSensorStations(1L);

        Mockito.verify(raspberryPiService).removeAvailableSensorStationAfterScanTimeOut(1L, 11L);
        Mockito.verify(raspberryPiService).removeAvailableSensorStationAfterScanTimeOut(1L, 12L);
    }

    @Test
    void cleanAvailableSensorStations_doesNotRemoveUnavailableStations() {
        RaspberryPi raspberryPi = new RaspberryPi();
        raspberryPi.setSensorStations(List.of(
                station(11L, DeviceStatus.ONLINE),
                station(12L, DeviceStatus.OFFLINE),
                station(13L, DeviceStatus.DECOMMISSIONED)
        ));

        Mockito.when(raspberryPiService.getByIdInternal(1L)).thenReturn(raspberryPi);

        cleaner.cleanAvailableSensorStations(1L);

        Mockito.verify(raspberryPiService).getByIdInternal(1L);
        Mockito.verify(raspberryPiService, Mockito.never())
                .removeAvailableSensorStationAfterScanTimeOut(Mockito.anyLong(), Mockito.anyLong());
    }

    private SensorStation station(Long id, DeviceStatus status) {
        SensorStation station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", id);
        station.setDeviceStatus(status);
        return station;
    }
}
