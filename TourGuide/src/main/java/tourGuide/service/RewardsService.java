package tourGuide.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

/**
 * Service class for rewards in TourGuide application
 * 
 */
@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtilService gpsUtilService;
	private final RewardCentral rewardsCentral;

	// ajout executor ici aussi?
	private ExecutorService executor = Executors.newFixedThreadPool(1200);

	/**
	 * Constructor for instancing a reward service
	 * 
	 * @param gpsUtilService - GpsUtil
	 * @param rewardCentral  - RewardCentral
	 */
	public RewardsService(GpsUtilService gpsUtilService, RewardCentral rewardCentral) {
		this.gpsUtilService = gpsUtilService;
		this.rewardsCentral = rewardCentral;
	}

	/**
	 * Set a proximityBuffer (distance between a location and an attraction)
	 * 
	 * @param proximityBuffer - int
	 */
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	/**
	 * Set a default proximityBuffer
	 */
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/**
	 * Calculate rewards for a specific user from attractions he has visited
	 * 
	 * @param user - User
	 */
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtilService.getAttractions();

		CompletableFuture.runAsync(() -> {
			for (VisitedLocation visitedLocation : userLocations) {
				for (Attraction attraction : attractions) {
					if (user.getUserRewards().stream()
							.filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
						if (nearAttraction(visitedLocation, attraction)) {
							user.addUserReward(
									new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						}
					}
				}
			}
		}, executor);
	}

	/**
	 * Verify if an attraction is in the range of a user location by returning true
	 * 
	 * @param attraction - Attraction
	 * @param location   - Location
	 * @return Boolean
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	/**
	 * Verify if an attraction is in the range of proximityBuffer by returning true
	 * 
	 * @param visitedLocation - VisitedLocation
	 * @param attraction      - Attraction
	 * @return Boolean
	 */
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	/**
	 * Get rewardPoints from a user and an attraction
	 * 
	 * @param attraction - Attraction
	 * @param user       - User
	 * @return rewardPoints - int
	 */
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	/**
	 * Get distance between two location
	 * 
	 * @param loc1 - Location
	 * @param loc2 - Location
	 * @return distance - double
	 */
	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math
				.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

}
