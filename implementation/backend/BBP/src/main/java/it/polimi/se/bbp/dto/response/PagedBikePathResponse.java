package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated bike path response.
 * Contains a page of bike paths along with pagination metadata.
 * Used to return a subset of user's bike paths with information about total available data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagedBikePathResponse {

    /**
     * List of bike paths in the current page.
     * Contains the actual bike path data with all details including points and obstacles.
     */
    private List<BikePathResponse> content;

    /**
     * Current page number (0-indexed).
     * First page is 0, second page is 1, etc.
     */
    private int currentPage;

    /**
     * Number of items per page (page size).
     * Example: 20 means each page contains up to 20 bike paths.
     */
    private int pageSize;

    /**
     * Total number of bike paths available across all pages.
     * Example: If user has 85 bike paths total, this will be 85.
     */
    private long totalElements;

    /**
     * Total number of pages available.
     * Calculated as: ceil(totalElements / pageSize)
     * Example: 85 bike paths with pageSize=20 results in 5 pages
     */
    private int totalPages;

    /**
     * Flag indicating if there is a next page available.
     * True if currentPage is less than totalPages minus 1
     */
    private boolean hasNext;

    /**
     * Flag indicating if there is a previous page available.
     * True if currentPage is greater than 0
     */
    private boolean hasPrevious;

    /**
     * Flag indicating if this is the first page.
     * Equivalent to: currentPage == 0
     */
    private boolean firstPage;

    /**
     * Flag indicating if this is the last page.
     * Equivalent to: currentPage == totalPages minus 1
     */
    private boolean lastPage;

}