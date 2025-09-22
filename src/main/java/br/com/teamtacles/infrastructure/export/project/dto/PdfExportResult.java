package br.com.teamtacles.infrastructure.export.project.dto;

import lombok.Data;

@Data
public class PdfExportResult {
    private final byte[] content;
    private final String filename;
}