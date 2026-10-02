package com.caselock.dto.request;

import com.caselock.entity.enums.EvidenceCategory;
import jakarta.validation.constraints.NotNull;

/**
 * Metadata submitted alongside the multipart file when registering new
 * evidence. Sent as a JSON string part named "metadata" in the multipart
 * request, parsed manually in the controller.
 */
public record EvidenceUploadMetadata(
        @NotNull(message = "Case id is required") Long caseId,
        String description,
        @NotNull(message = "Evidence category is required") EvidenceCategory evidenceCategory
) {
}
