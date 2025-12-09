package it.polimi.se.bbp.dto.response;

import java.util.List;

/**
 * Response for paginated bike paths with navigation metadata.
 * Returns a subset of bike paths with pagination information.
 * @param content list of bike paths in current page (with all details including points and obstacles)
 * @param currentPage current page number (0-indexed)
 * @param pageSize number of items per page
 * @param totalElements total number of bike paths across all pages
 * @param totalPages total number of pages (calculated as ceil(totalElements / pageSize))
 * @param hasNext flag indicating if next page is available
 * @param hasPrevious flag indicating if previous page is available
 * @param firstPage flag indicating if this is the first page (currentPage == 0)
 * @param lastPage flag indicating if this is the last page (currentPage == totalPages - 1)
 */
public record PagedBikePathResponse(

        /*
         * List of bike paths in current page.
         * Includes all details (points and obstacles).
         */
        List<BikePathResponse> content,

        /*
         * Current page number (0-indexed).
         */
        int currentPage,

        /*
         * Number of items per page.
         */
        int pageSize,

        /*
         * Total number of bike paths across all pages.
         */
        long totalElements,

        /*
         * Total number of pages.
         * Calculated as: ceil(totalElements / pageSize)
         */
        int totalPages,

        /*
         * Flag indicating if next page is available.
         */
        boolean hasNext,

        /*
         * Flag indicating if previous page is available.
         */
        boolean hasPrevious,

        /*
         * Flag indicating if this is the first page.
         */
        boolean firstPage,

        /*
         * Flag indicating if this is the last page.
         */
        boolean lastPage

) {}