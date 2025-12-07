package it.polimi.se.bbp.dto.response;

import java.util.List;

/**
 * Response for paginated trips with navigation metadata.
 * Returns a subset of trips with pagination information.
 * @param content list of trips in current page (with all details including points and meteorological data)
 * @param currentPage current page number (0-indexed)
 * @param pageSize number of items per page
 * @param totalElements total number of trips across all pages
 * @param totalPages total number of pages (calculated as ceil(totalElements / pageSize))
 * @param hasNext flag indicating if next page is available
 * @param hasPrevious flag indicating if previous page is available
 * @param firstPage flag indicating if this is the first page (currentPage == 0)
 * @param lastPage flag indicating if this is the last page (currentPage == totalPages - 1)
 */
public record PagedTripResponse(

        /*
         * List of trips in current page.
         * Includes all details (points and meteorological data).
         */
        List<TripResponse> content,

        /*
         * Current page number (0-indexed).
         */
        int currentPage,

        /*
         * Number of items per page.
         */
        int pageSize,

        /*
         * Total number of trips across all pages.
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