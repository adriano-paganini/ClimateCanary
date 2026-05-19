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

    private SensorStation station(Long id, DeviceStatus status) {
        SensorStation station = new SensorStation();
        station.setId(id);
        station.setDeviceStatus(status);
        return station;
    }
}
