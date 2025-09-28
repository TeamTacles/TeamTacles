package br.com.teamtacles.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "PdfExportResult", description = "DTO containing the result of a PDF export operation.")
public class PdfExportResult {

    @Schema(description = "The raw byte content of the generated PDF file.")
    private final byte[] content;

    @Schema(description = "The suggested filename for the exported PDF.", example = "project_report_2023-10-27.pdf")
    private final String filename;
}