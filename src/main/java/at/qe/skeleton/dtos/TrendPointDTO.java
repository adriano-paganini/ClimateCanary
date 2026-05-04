package at.qe.skeleton.dtos;

import java.time.LocalDateTime;

public record TrendPointDTO(
        LocalDateTime timestamp,
        Long value
) {}
