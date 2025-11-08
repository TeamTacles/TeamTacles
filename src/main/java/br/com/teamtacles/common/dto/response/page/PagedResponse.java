package br.com.teamtacles.common.dto.response.page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(name = "PagedResponse", description = "DTO for paginated responses.")
public class PagedResponse<T> {

    @Schema(name = "content", description = "List of items for the current page")
    private List<T> content;

    @Schema(name = "page", description = "Current page number (0-indexed)")
    private int page;
    @Schema(name = "size", description = "Number of items per page")
    private int size;
    @Schema(name = "totalElements", description = "Total number of items across all pages")
    private long totalElements;
    @Schema(name = "totalPages", description = "Total number of pages available")
    private int totalPages;
    @Schema(name = "last", description = "Indicates if this is the last page")
    private boolean last;
}