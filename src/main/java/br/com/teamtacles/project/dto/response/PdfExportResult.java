package br.com.teamtacles.project.dto.response;

import lombok.Data;

@Data
public class PdfExportResult {
    private final byte[] content;
    private final String filename;
}