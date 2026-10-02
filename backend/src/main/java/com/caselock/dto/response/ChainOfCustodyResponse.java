package com.caselock.dto.response;

import com.caselock.entity.ChainOfCustodyEvent;
import com.caselock.entity.enums.CustodyAction;

import java.time.LocalDateTime;

public record ChainOfCustodyResponse(
        Long id,
        Long evidenceId,
        String evidenceNumber,
        String fromUserName,
        String toUserName,
        CustodyAction action,
        LocalDateTime eventTimestamp,
        String reason,
        String location,
        String remarks,
        String hashAtTransfer,
        String ipAddress
) {
    public static ChainOfCustodyResponse from(ChainOfCustodyEvent event) {
        return new ChainOfCustodyResponse(
                event.getId(),
                event.getEvidence() != null ? event.getEvidence().getId() : null,
                event.getEvidence() != null ? event.getEvidence().getEvidenceNumber() : null,
                event.getFromUser() != null ? event.getFromUser().getFullName() : null,
                event.getToUser() != null ? event.getToUser().getFullName() : null,
                event.getAction(),
                event.getEventTimestamp(),
                event.getReason(),
                event.getLocation(),
                event.getRemarks(),
                event.getHashAtTransfer(),
                event.getIpAddress()
        );
    }
}
