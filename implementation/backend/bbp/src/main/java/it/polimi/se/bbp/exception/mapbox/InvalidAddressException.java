package it.polimi.se.bbp.exception.mapbox;

import lombok.Getter;

/**
 * Thrown when an address cannot be geocoded because it is invalid or not found.
 * Indicates user error: provided address not found in Mapbox database.
 * Should result in HTTP 400 Bad Request.
 */
@Getter
public class InvalidAddressException extends MapboxClientException {

    /**
     * Address that could not be geocoded.
     */
    private final String address;

    /**
     * Constructs exception for invalid address.
     * @param address address that could not be geocoded
     */
    public InvalidAddressException(String address) {
        super(String.format("Unable to geocode address: '%s'. Please verify the address is correct.", address));
        this.address = address;
    }

    /**
     * Constructs exception for invalid address with cause.
     * @param address address that could not be geocoded
     * @param cause underlying cause of this exception
     */
    public InvalidAddressException(String address, Throwable cause) {
        super(String.format("Unable to geocode address: '%s'. Please verify the address is correct.", address), cause);
        this.address = address;
    }

}