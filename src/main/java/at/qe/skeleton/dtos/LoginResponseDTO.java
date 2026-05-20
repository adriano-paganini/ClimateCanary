package at.qe.skeleton.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "JWT bearer token returned after successful authentication")
        String bearerToken
) {
}
