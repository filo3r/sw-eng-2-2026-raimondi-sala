package it.polimi.se.bbp.exception.obstacle;

/**
 * Thrown when obstacle location is too far from bike path route.
 * Occurs when geocoded obstacle address falls outside acceptable buffer zone around path.
 */
public class ObstacleTooFarException extends ObstacleException {

    /**
     * Constructs exception for obstacle too far from bike path.
     * @param message detail message explaining why obstacle was rejected
     */
    public ObstacleTooFarException(String message) {
        super(message);
    }

}