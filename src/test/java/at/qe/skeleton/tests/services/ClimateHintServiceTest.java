package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ClimateHintUpdateDTO;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.services.ClimateHintService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClimateHintServiceTest {

    @Mock
    private ClimateHintRepository climateHintRepository;

    @InjectMocks
    private ClimateHintService climateHintService;

    private ClimateHint hint;

    @BeforeEach
    void setUp() {
        hint = new ClimateHint();
        hint.setMetric(Metric.TEMPERATURE);
        hint.setHintText("Open a window when temperature exceeds 26°C.");
    }


    @Test
    @DisplayName("Get all climate hints returns all")
    void findAll_returnsAllHints() {
        ClimateHint second = new ClimateHint();
        Mockito.when(climateHintRepository.findAll()).thenReturn(List.of(hint, second));

        List<ClimateHint> result = climateHintService.findAll();

        Assertions.assertThat(result).hasSize(2).containsExactly(hint, second);
    }

    @Test
    @DisplayName("Get all climate hints empty returns empty list")
    void findAll_empty_returnsEmptyList() {
        Mockito.when(climateHintRepository.findAll()).thenReturn(List.of());

        List<ClimateHint> result = climateHintService.findAll();

        Assertions.assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Get climate hint by id returns hint")
    void getClimateHintById_exists_returnsHint() {
        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));

        ClimateHint result = climateHintService.getClimateHintById(1L);

        Assertions.assertThat(result).isEqualTo(hint);
    }

    @Test
    @DisplayName("Get climate hint by id throws when not found")
    void getClimateHintById_notFound_throwsNotFoundException() {
        Mockito.when(climateHintRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> climateHintService.getClimateHintById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }


    @Test
    @DisplayName("Create climate hint with valid data saves and returns")
    void create_validHint_savesAndReturns() {
        Mockito.when(climateHintRepository.save(hint)).thenReturn(hint);

        ClimateHint result = climateHintService.create(hint);

        Assertions.assertThat(result).isEqualTo(hint);
        Mockito.verify(climateHintRepository).save(hint);
    }

    @Test
    @DisplayName("Create climate hint returns persisted entity from repository")
    void create_persistsReturnedEntityFromRepository() {
        ClimateHint saved = new ClimateHint();
        saved.setMetric(Metric.HUMIDITY);
        saved.setHintText("Ventilate when humidity is high.");
        Mockito.when(climateHintRepository.save(hint)).thenReturn(saved);

        ClimateHint result = climateHintService.create(hint);

        Assertions.assertThat(result.getId()).isEqualTo(hint.getId());
        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.HUMIDITY);
    }


    @Test
    @DisplayName("Update climate hint with both fields updates all fields")
    void update_bothFieldsProvided_updatesAndReturns() {
        ClimateHintUpdateDTO dto = new ClimateHintUpdateDTO(Metric.IAQ, "Ventilate when CO2 is high.");

        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));
        Mockito.when(climateHintRepository.save(hint)).thenReturn(hint);

        ClimateHint result = climateHintService.update(1L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.IAQ);
        Assertions.assertThat(result.getHintText()).isEqualTo("Ventilate when CO2 is high.");
        Mockito.verify(climateHintRepository).save(hint);
    }

    @Test
    @DisplayName("Update climate hint with only metric updates metric only")
    void update_onlyMetricProvided_updatesOnlyMetric() {
        ClimateHintUpdateDTO dto = new ClimateHintUpdateDTO(Metric.HUMIDITY, null);

        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));
        Mockito.when(climateHintRepository.save(hint)).thenReturn(hint);

        ClimateHint result = climateHintService.update(1L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.HUMIDITY);
        Assertions.assertThat(result.getHintText()).isEqualTo("Open a window when temperature exceeds 26°C.");
    }

    @Test
    @DisplayName("Update climate hint with only hint text updates hint text only")
    void update_onlyHintTextProvided_updatesOnlyHintText() {
        ClimateHintUpdateDTO dto = new ClimateHintUpdateDTO(null, "New hint text.");

        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));
        Mockito.when(climateHintRepository.save(hint)).thenReturn(hint);

        ClimateHint result = climateHintService.update(1L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getHintText()).isEqualTo("New hint text.");
    }

    @Test
    @DisplayName("Update climate hint with null fields does not modify values")
    void update_allNullFields_doesNotModifyExistingValues() {
        ClimateHintUpdateDTO dto = new ClimateHintUpdateDTO(null, null);

        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));
        Mockito.when(climateHintRepository.save(hint)).thenReturn(hint);

        ClimateHint result = climateHintService.update(1L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getHintText()).isEqualTo("Open a window when temperature exceeds 26°C.");
    }

    @Test
    @DisplayName("Update climate hint with non existing id throws not found")
    void update_notFound_throwsNotFoundException() {
        Mockito.when(climateHintRepository.findById(99L)).thenReturn(Optional.empty());

        ClimateHintUpdateDTO dto = new ClimateHintUpdateDTO(Metric.IAQ, "Some text.");

        Assertions.assertThatThrownBy(() -> climateHintService.update(99L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(climateHintRepository, Mockito.never()).save(Mockito.any());
    }


    @Test
    @DisplayName("Delete climate hint clears thresholds and deletes")
    void delete_exists_clearsThresholdsAndDeletes() {
        hint.getThresholds().add(new Threshold());
        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));

        climateHintService.delete(1L);

        Assertions.assertThat(hint.getThresholds()).isEmpty();
        Mockito.verify(climateHintRepository).delete(hint);
    }

    @Test
    @DisplayName("Delete climate hint with no thresholds deletes successfully")
    void delete_existsWithNoThresholds_deletesSuccessfully() {
        Mockito.when(climateHintRepository.findById(1L)).thenReturn(Optional.of(hint));

        climateHintService.delete(1L);

        Mockito.verify(climateHintRepository).delete(hint);
    }

    @Test
    @DisplayName("Delete climate hint with non existing id throws not found")
    void delete_notFound_throwsNotFoundExceptionAndSkipsDelete() {
        Mockito.when(climateHintRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> climateHintService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(climateHintRepository, Mockito.never()).delete(Mockito.any());
    }
}