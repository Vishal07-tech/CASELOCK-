package com.caselock.service;

import com.caselock.entity.Case;
import com.caselock.entity.enums.Role;
import com.caselock.security.UserPrincipal;
import org.springframework.stereotype.Service;

/**
 * Central place for the "can this user see/touch this case?" rule so it is
 * applied consistently by CaseService, EvidenceService, ChainOfCustodyService
 * and ReportService rather than re-implemented in each one.
 *
 * ADMIN, LEGAL_OFFICER, FORENSIC_ANALYST and VIEWER have organization-wide
 * read access to cases (their write actions are separately restricted by
 * @PreAuthorize on the controllers). INVESTIGATOR is restricted to cases
 * they lead or are a team member on.
 */
@Service
public class CaseAccessService {

    public boolean canAccess(UserPrincipal principal, Case caseEntity) {
        if (principal == null || caseEntity == null) {
            return false;
        }
        Role role = Role.valueOf(principal.getRole());
        if (role != Role.INVESTIGATOR) {
            return true;
        }
        if (caseEntity.getInvestigator() != null && caseEntity.getInvestigator().getId().equals(principal.getId())) {
            return true;
        }
        return caseEntity.getAssignedTeam() != null && caseEntity.getAssignedTeam().stream()
                .anyMatch(u -> u.getId().equals(principal.getId()));
    }
}
