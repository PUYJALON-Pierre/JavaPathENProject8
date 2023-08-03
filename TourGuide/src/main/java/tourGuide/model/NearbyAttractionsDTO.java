package tourGuide.model;

import gpsUtil.location.Location;
import lombok.Data;


/**
 * Object class to return informations about a nearby attraction
 * 
 */
@Data
public class NearbyAttractionsDTO {

	String attractionName;

	Location attractionLocation;

	Location userLocation;

	Double distanceBetweenUserLocationAndAttractionInMiles;

	int rewardPoints;

}
