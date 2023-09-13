package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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

	private ExecutorService executorService = Executors.newFixedThreadPool(1200);
	
	public Executor getExecutor(){
		return this.executorService;
	}

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

	public RewardCentral getRewardsCentral(){
		return rewardsCentral;
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
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public CompletableFuture<Void> calculateRewards(User user) {
		
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		
		List<Attraction> attractions = gpsUtilService.getListOfAttractions();



		List<CompletableFuture<Void>> futures = new ArrayList<>();
		
		for (Attraction attraction : attractions) {

			for (VisitedLocation visitedLocation : userLocations) {

				if (nearAttraction(visitedLocation, attraction)) {
					//future captures the result of asynchronous task
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}, executorService);
					//The task execution will be handled by the executorService, which is an ExecutorService instance passed as an argument.
					futures.add(future);
					break;
				}
			}
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		

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
	 * @throws ExecutionException
	 * @throws InterruptedException
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
