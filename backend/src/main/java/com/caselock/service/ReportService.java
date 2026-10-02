package com.caselock.service;

import com.caselock.dto.response.AuditLogResponse;
import com.caselock.dto.response.CaseReportResponse;
import com.caselock.dto.response.CaseResponse;
import com.caselock.dto.response.ChainOfCustodyResponse;
import com.caselock.dto.response.EvidenceResponse;
import com.caselock.dto.response.IntegrityVerificationResponse;
import com.caselock.entity.Case;
import com.caselock.entity.Evidence;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.repository.AuditLogRepository;
import com.caselock.repository.ChainOfCustodyEventRepository;
import com.caselock.repository.EvidenceRepository;
import com.caselock.util.SecurityUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CaseService caseService;
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyEventRepository custodyEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CaseReportResponse buildReport(Long caseId, String ipAddress) {
        Case caseEntity = caseService.findCaseOrThrow(caseId);
        caseService.assertAccess(caseEntity, "generate a report for");

        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityId(caseId);

        List<EvidenceResponse> evidenceResponses = evidenceList.stream()
                .map(EvidenceResponse::from).collect(Collectors.toList());

        List<ChainOfCustodyResponse> custodyResponses = evidenceList.stream()
                .flatMap(e -> custodyEventRepository.findByEvidenceIdOrderByEventTimestampAsc(e.getId()).stream())
                .map(ChainOfCustodyResponse::from)
                .collect(Collectors.toList());

        List<IntegrityVerificationResponse> integrityResults = evidenceList.stream()
                .map(e -> new IntegrityVerificationResponse(
                        e.getId(), e.getEvidenceNumber(), e.getOriginalSha256(), e.getCurrentSha256(),
                        e.getIntegrityStatus(), e.getIntegrityStatus() == com.caselock.entity.enums.IntegrityStatus.VERIFIED,
                        e.getLastVerifiedAt(),
                        e.getIntegrityStatus().name()))
                .collect(Collectors.toList());

        List<AuditLogResponse> auditHistory = auditLogRepository
                .search(null, null, "Case", caseId, null, null, null, org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent().stream().map(AuditLogResponse::from).collect(Collectors.toList());

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.REPORT_GENERATED, "Case", caseId,
                "Case report generated for " + caseEntity.getCaseNumber() + ".", ipAddress, AuditResult.SUCCESS);

        return new CaseReportResponse(
                CaseResponse.from(caseEntity),
                evidenceResponses,
                custodyResponses,
                integrityResults,
                auditHistory,
                LocalDateTime.now(),
                SecurityUtil.currentUsername()
        );
    }

    @Transactional(readOnly = true)
    public byte[] buildPdfReport(Long caseId, String ipAddress) {
        CaseReportResponse report = buildReport(caseId, ipAddress);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdfDoc);
            doc.setMargins(36, 36, 36, 36);

            // Document Title & Metadata
            doc.add(new Paragraph("CaseLock - Forensic Case Report").setBold().setFontSize(18));
            doc.add(new Paragraph("Generated on " + report.generatedAt().format(fmt) + " by user: " + report.generatedBy())
                    .setFontSize(9).setFontColor(ColorConstants.DARK_GRAY));
            doc.add(new Paragraph(" "));

            // 1. Case Information
            CaseResponse c = report.caseInfo();
            doc.add(new Paragraph("1. Case Information").setBold().setFontSize(13).setFontColor(ColorConstants.BLACK));
            
            String teamMembers = (c.assignedTeam() != null && !c.assignedTeam().isEmpty())
                    ? c.assignedTeam().stream().map(com.caselock.dto.response.UserResponse::fullName).collect(Collectors.joining(", "))
                    : "None assigned";

            doc.add(infoTable(new String[][]{
                    {"Case Number", c.caseNumber()},
                    {"Title", c.title()},
                    {"Case Type", c.caseType() != null ? c.caseType().name() : "-"},
                    {"Priority Level", c.priority() != null ? c.priority().name() : "-"},
                    {"Case Status", c.status() != null ? c.status().name() : "-"},
                    {"Lead Investigator", c.investigator() != null ? c.investigator().fullName() : "-"},
                    {"Assigned Team", teamMembers},
                    {"Location / Unit", c.location() != null && !c.location().isBlank() ? c.location() : "-"},
                    {"Case Start Date", c.caseStartDate() != null ? c.caseStartDate().format(dateFmt) : "-"},
                    {"Case Closing Date", c.caseClosingDate() != null ? c.caseClosingDate().format(dateFmt) : "-"}
            }));

            // Case Description
            doc.add(new Paragraph("Case Description / Incident Summary:").setBold().setFontSize(10).setMarginTop(8));
            String caseDesc = (c.description() != null && !c.description().isBlank() && !"null".equalsIgnoreCase(c.description()))
                    ? c.description()
                    : "No specific description was recorded for this case.";
            doc.add(new Paragraph(caseDesc).setFontSize(9).setItalic().setMarginBottom(10)
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(245, 247, 250)).setPadding(8));

            // 2. Evidence Summary & Submission Details
            doc.add(new Paragraph("2. Submitted Evidence Items (" + report.evidenceSummary().size() + ")")
                    .setBold().setFontSize(13).setMarginTop(12));

            if (report.evidenceSummary().isEmpty()) {
                doc.add(new Paragraph("No evidence items have been submitted or registered for this case.")
                        .setFontSize(9).setFontColor(ColorConstants.GRAY).setItalic().setMarginBottom(8));
            } else {
                Table evTable = new Table(UnitValue.createPercentArray(new float[]{2.2f, 3f, 2f, 1.8f, 2.5f, 2.5f, 2f})).useAllAvailableWidth();
                addHeaderRow(evTable, "Evidence #", "File Name", "Category", "Size", "Uploaded By", "Upload Date", "Status");
                for (EvidenceResponse e : report.evidenceSummary()) {
                    evTable.addCell(cell(e.evidenceNumber()));
                    evTable.addCell(cell(e.fileName()));
                    evTable.addCell(cell(e.evidenceCategory() != null ? e.evidenceCategory().name() : "-"));
                    evTable.addCell(cell(formatFileSize(e.fileSizeBytes())));
                    evTable.addCell(cell(e.uploadedBy() != null ? e.uploadedBy().fullName() : "-"));
                    evTable.addCell(cell(e.uploadDate() != null ? e.uploadDate().format(fmt) : "-"));
                    evTable.addCell(cell((e.status() != null ? e.status().name() : "-") + " (" + (e.integrityStatus() != null ? e.integrityStatus().name() : "-") + ")"));
                }
                doc.add(evTable);

                // Detailed Evidence Descriptions
                doc.add(new Paragraph("Evidence Details & Submission Notes:").setBold().setFontSize(10).setMarginTop(8));
                for (EvidenceResponse e : report.evidenceSummary()) {
                    String evDesc = (e.description() != null && !e.description().isBlank() && !"null".equalsIgnoreCase(e.description()))
                            ? e.description() : "No description provided at upload.";
                    String custodian = e.currentCustodian() != null ? e.currentCustodian().fullName() : "-";
                    doc.add(new Paragraph("• " + e.evidenceNumber() + " (" + e.fileName() + "):")
                            .setBold().setFontSize(9).setMarginTop(3));
                    doc.add(new Paragraph("   Description: " + evDesc + " | Current Custodian: " + custodian
                            + " | Sealed: " + (e.sealed() ? "YES" : "NO"))
                            .setFontSize(8.5f).setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(3));
                }
            }

            // 3. Chain of Custody
            doc.add(new Paragraph("3. Chain of Custody Record (" + report.chainOfCustody().size() + " events)")
                    .setBold().setFontSize(13).setMarginTop(12));

            if (report.chainOfCustody().isEmpty()) {
                doc.add(new Paragraph("No custody transfer events recorded for this case.")
                        .setFontSize(9).setFontColor(ColorConstants.GRAY).setItalic().setMarginBottom(8));
            } else {
                Table custodyTable = new Table(UnitValue.createPercentArray(new float[]{2.5f, 2f, 2f, 2.5f, 2.5f, 3f})).useAllAvailableWidth();
                addHeaderRow(custodyTable, "Timestamp", "Evidence #", "Action", "From", "To", "Reason / Remarks");
                for (ChainOfCustodyResponse ev : report.chainOfCustody()) {
                    custodyTable.addCell(cell(ev.eventTimestamp() != null ? ev.eventTimestamp().format(fmt) : "-"));
                    custodyTable.addCell(cell(ev.evidenceNumber() != null ? ev.evidenceNumber() : "-"));
                    custodyTable.addCell(cell(ev.action() != null ? ev.action().name() : "-"));
                    custodyTable.addCell(cell(ev.fromUserName() != null ? ev.fromUserName() : "-"));
                    custodyTable.addCell(cell(ev.toUserName() != null ? ev.toUserName() : "-"));
                    String reasonRemarks = (ev.reason() != null ? ev.reason() : "")
                            + (ev.remarks() != null && !ev.remarks().isBlank() ? " (" + ev.remarks() + ")" : "");
                    custodyTable.addCell(cell(reasonRemarks.isBlank() ? "-" : reasonRemarks));
                }
                doc.add(custodyTable);
            }

            // 4. Integrity Verification Results
            doc.add(new Paragraph("4. Cryptographic Hash Integrity Baseline")
                    .setBold().setFontSize(13).setMarginTop(12));

            if (report.integrityResults().isEmpty()) {
                doc.add(new Paragraph("No evidence items available for integrity verification.")
                        .setFontSize(9).setFontColor(ColorConstants.GRAY).setItalic().setMarginBottom(8));
            } else {
                Table integrityTable = new Table(UnitValue.createPercentArray(new float[]{2.2f, 4f, 4f, 2f})).useAllAvailableWidth();
                addHeaderRow(integrityTable, "Evidence #", "Original SHA-256 (Baseline)", "Current SHA-256", "Integrity Status");
                for (IntegrityVerificationResponse r : report.integrityResults()) {
                    integrityTable.addCell(cell(r.evidenceNumber()));
                    integrityTable.addCell(cell(r.originalSha256() != null ? r.originalSha256() : "-"));
                    integrityTable.addCell(cell(r.currentSha256() != null ? r.currentSha256() : "-"));
                    integrityTable.addCell(cell(r.integrityStatus() != null ? r.integrityStatus().name() : "-"));
                }
                doc.add(integrityTable);
            }

            // 5. Audit History
            doc.add(new Paragraph("5. Audit Log History (" + report.auditHistory().size() + " logged actions)")
                    .setBold().setFontSize(13).setMarginTop(12));

            if (report.auditHistory().isEmpty()) {
                doc.add(new Paragraph("No audit history entries found for this case.")
                        .setFontSize(9).setFontColor(ColorConstants.GRAY).setItalic().setMarginBottom(8));
            } else {
                Table auditTable = new Table(UnitValue.createPercentArray(new float[]{2.5f, 2f, 4.5f, 1.5f})).useAllAvailableWidth();
                addHeaderRow(auditTable, "Timestamp", "User", "Action & Description", "Result");
                for (AuditLogResponse a : report.auditHistory()) {
                    auditTable.addCell(cell(a.eventTimestamp() != null ? a.eventTimestamp().format(fmt) : "-"));
                    auditTable.addCell(cell(a.username()));
                    auditTable.addCell(cell(a.description()));
                    auditTable.addCell(cell(String.valueOf(a.result())));
                }
                doc.add(auditTable);
            }

            // Legal Footer Note
            doc.add(new Paragraph(
                    "LEGAL NOTICE: A matching SHA-256 hash confirms the digital content of evidence items has not been altered "
                            + "since initial registration. This document constitutes an official evidentiary record produced by CaseLock.")
                    .setFontSize(8).setFontColor(ColorConstants.GRAY).setMarginTop(18));

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new com.caselock.exception.BadRequestException("Something went wrong while generating the PDF report. Please try again.");
        }
    }

    private Table infoTable(String[][] rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 3.5f})).useAllAvailableWidth();
        for (String[] row : rows) {
            table.addCell(new Cell().add(new Paragraph(row[0]).setBold().setFontSize(9)).setBorder(null).setPadding(2));
            table.addCell(new Cell().add(new Paragraph(row[1] != null && !row[1].isBlank() ? row[1] : "-").setFontSize(9)).setBorder(null).setPadding(2));
        }
        return table;
    }

    private void addHeaderRow(Table table, String... headers) {
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setBold().setFontSize(8.5f))
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(230, 235, 245)).setPadding(4));
        }
    }

    private Cell cell(String value) {
        return new Cell().add(new Paragraph(value != null && !value.isBlank() ? value : "-").setFontSize(8)).setPadding(3);
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
