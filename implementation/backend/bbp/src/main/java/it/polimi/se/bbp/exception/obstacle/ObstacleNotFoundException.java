package it.polimi.se.bbp.exception.obstacle;

import lombok.Getter;

/**
 * Thrown when attempting to update an obstacle not belonging to specified bike path.
 * Obstacle may not exist or may belong to different bike path.
 */
@Getter
public class ObstacleNotFoundException extends ObstacleException {

    /**
     * Constructs exception for obstacle not found.
     * @param message detail message explaining why obstacle was not found
     */
    public ObstacleNotFoundException(String message) {
        super(message);
    }

}