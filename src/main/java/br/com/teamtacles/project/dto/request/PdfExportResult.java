package br.com.teamtacles.project.dto.request;

import lombok.Data;

@Data
public class PdfExportResult {
    private final byte[] content;
    private final String filename;
}