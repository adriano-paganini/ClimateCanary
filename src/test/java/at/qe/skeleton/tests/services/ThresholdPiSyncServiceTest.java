package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdType;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.ThresholdPiSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ThresholdPiSyncServiceTest {

    @Mock
    private RaspberryPiServerService raspberryPiServerService;

    @InjectMocks
    private ThresholdPiSyncService thresholdPiSyncService;

    @Test
    @DisplayName("create sync sends insertion request with threshold DTO and climate hints to target Raspberry Pi")
    void synchronize_create_sendsInsertRequestToRaspberryPi() {
        ThresholdDTO thresholdDTO = thresholdDTO(Metric.TEMPERATURE, 25.0F, ThresholdType.UPPER);
        ClimateHintDTO hintDTO = new ClimateHintDTO(10L, Metric.TEMPERATURE, "Open a window");

        Mockito.when(raspberryPiServerService.informAboutNewThresholds(Mockito.eq(5L), Mockito.anyMap()))
                .thenReturn(PiRequestResult.SUCCESS);

        thresholdPiSyncService.synchronize(
                100L,
                null,
                null,
                5L,
                thresholdDTO,
                List.of(hintDTO),
                true,
                List.of()
        );

        ArgumentCaptor<Map<ThresholdDTO, List<ClimateHintDTO>>> captor = ArgumentCaptor.captor();
        Mockito.verify(raspberryPiServerService).informAboutNewThresholds(Mockito.eq(5L), captor.capture());
        Mockito.verify(raspberryPiServerService, Mockito.never()).deleteThresholds(Mockito.anyLong(), Mockito.anyList());

        assertThat(captor.getValue()).containsExactly(Map.entry(thresholdDTO, List.of(hintDTO)));
    }

    @Test
    @DisplayName("update sync deletes old threshold before sending updated threshold to Raspberry Pi")
    void synchronize_update_sendsDeleteThenInsertRequestsToRaspberryPi() {
        ThresholdDTO oldThresholdDTO = thresholdDTO(Metric.TEMPERATURE, 25.0F, ThresholdType.UPPER);
        ThresholdDTO updatedThresholdDTO = thresholdDTO(Metric.HUMIDITY, 50.0F, ThresholdType.LOWER);

        Mockito.when(raspberryPiServerService.deleteThresholds(5L, List.of(oldThresholdDTO)))
                .thenReturn(PiRequestResult.SUCCESS);
        Mockito.when(raspberryPiServerService.informAboutNewThresholds(Mockito.eq(5L), Mockito.anyMap()))
                .thenReturn(PiRequestResult.SUCCESS);

        thresholdPiSyncService.synchronize(
                100L,
                5L,
                oldThresholdDTO,
                5L,
                updatedThresholdDTO,
                List.of(),
                true,
                List.of()
        );

        Mockito.verify(raspberryPiServerService).deleteThresholds(5L, List.of(oldThresholdDTO));

        ArgumentCaptor<Map<ThresholdDTO, List<ClimateHintDTO>>> captor = ArgumentCaptor.captor();
        Mockito.verify(raspberryPiServerService).informAboutNewThresholds(Mockito.eq(5L), captor.capture());
        assertThat(captor.getValue()).containsExactly(Map.entry(updatedThresholdDTO, List.of()));
    }

    @Test
    @DisplayName("delete sync sends deletion request with old threshold DTO to Raspberry Pi")
    void synchronize_delete_sendsDeleteRequestToRaspberryPi() {
        ThresholdDTO oldThresholdDTO = thresholdDTO(Metric.TEMPERATURE, 25.0F, ThresholdType.UPPER);

        Mockito.when(raspberryPiServerService.deleteThresholds(5L, List.of(oldThresholdDTO)))
                .thenReturn(PiRequestResult.SUCCESS);

        thresholdPiSyncService.synchronize(
                100L,
                5L,
                oldThresholdDTO,
                null,
                null,
                List.of(),
                false,
                List.of()
        );

        Mockito.verify(raspberryPiServerService).deleteThresholds(5L, List.of(oldThresholdDTO));
        Mockito.verify(raspberryPiServerService, Mockito.never())
                .informAboutNewThresholds(Mockito.anyLong(), Mockito.anyMap());
    }

    private ThresholdDTO thresholdDTO(Metric metric, Float boundValue, ThresholdType thresholdType) {
        return new ThresholdDTO(
                100L,
                1L,
                metric,
                boundValue,
                thresholdType,
                List.of(10L),
                true
        );
    }
}
