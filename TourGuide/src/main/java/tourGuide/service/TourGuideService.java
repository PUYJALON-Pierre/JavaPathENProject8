package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.NearbyAttractionsDTO;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Service class for TourGuide application features
 * 
 */
@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtilService gpsUtilService;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	private ExecutorService executorService = Executors.newFixedThreadPool(1200);

	/**
	 * Constructor for instancing a TourGuideService
	 * 
	 * @param gpsUtilService - GpsUtilService
	 * @param rewardsService - RewardsService
	 */
	public TourGuideService(GpsUtilService gpsUtilService, RewardsService rewardsService) {
		this.gpsUtilService = gpsUtilService;
		this.rewardsService = rewardsService;

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	/**
	 * Get userRewards from a specific User
	 * 
	 * @param user - User
	 * @return List of UserReward
	 */
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * Get last visited location with informations of a specific user
	 * 
	 * @param user - User
	 * @return last visited location with informations - VisitedLocation
	 */
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).join();
		return visitedLocation;
	}

	/**
	 * Get a user by his userName
	 * 
	 * @param userName - String
	 * @return user - User
	 */
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	/**
	 * Get all users
	 * 
	 * @return List of User
	 */
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	/**
	 * Adding a user
	 * 
	 * @param user - User
	 */
	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	/**
	 * 
	 * Get tripDeals for a user
	 * 
	 * @param user - User
	 * @return tripDeals - List of Provider
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Track current user location
	 * 
	 * @param user - User
	 * @return CompletableFuture VisitedLocation
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		CompletableFuture<VisitedLocation> visitedLocationCompletableFuture = CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocation = gpsUtilService.getUserLocation(user.getUserId());
			return visitedLocation;
		}, executorService).thenApplyAsync((visitedLocation) -> {//async : new thread forked
			user.addToVisitedLocations(visitedLocation);
			rewardsService.calculateRewards(user).join();
			//wait calculateReward is over before returning loc.
			return visitedLocation;
		}, rewardsService.getExecutor());
		return visitedLocationCompletableFuture;
	}

	/**
	 * Track current location of users from a list
	 * 
	 * @param users - List of User
	 * @return CompletableFuture void
	 */
	public CompletableFuture<Void> trackAllUserLocation(List<User> users) {
		List<CompletableFuture<VisitedLocation>> completableFutures = users.stream()
				.map(user -> this.trackUserLocation(user)).collect(Collectors.toList());
		return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));

	}
	
/**
 * Get 5 nearest attractions from a VisitedLocation
 * 
 * @param visitedLocation - VisitedLocation
 * @return nearbyAttractionsDTOList - List<NearbyAttractionsDTO>
 */
	public List<NearbyAttractionsDTO> getNearByAttractions(VisitedLocation visitedLocation) {
		List<NearbyAttractionsDTO> nearbyAttractionsDTOList = new ArrayList<>();

		// Retrieve list of Attractions sorted by distance from user
		List<Attraction> attractions = gpsUtilService.getListOfAttractions().stream()
				.sorted(Comparator.comparingDouble(a -> rewardsService.getDistance(visitedLocation.location, a)))
				.limit(5).collect(Collectors.toList());

		for (Attraction attraction : attractions) {

			// Create new attraction DTO
			NearbyAttractionsDTO nearbyAttractionDTO = new NearbyAttractionsDTO();

			// Set DTO attributes
			nearbyAttractionDTO.setAttractionName(attraction.attractionName);
			Location attractionLocation = new Location(attraction.latitude, attraction.longitude);
			nearbyAttractionDTO.setAttractionLocation(attractionLocation);
			nearbyAttractionDTO.setUserLocation(visitedLocation.location);
			nearbyAttractionDTO.setDistanceBetweenUserLocationAndAttractionInMiles(
					rewardsService.getDistance(visitedLocation.location, attraction));

			nearbyAttractionDTO.setRewardPoints(
					rewardsService.getRewardsCentral().getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId));

			// Add DTO to list
			nearbyAttractionsDTOList.add(nearbyAttractionDTO);
		}

		return nearbyAttractionsDTOList;
	}

	/**
	 * Get current locations of all users
	 * 
	 * @return lastVisitedLocations - Map<String, Location>
	 */
	public Map<String, Location> getAllCurrentLocations() {

		Map<String, Location> lastVisitedLocations = new HashMap<String, Location>();
		List<User> userList = getAllUsers();
		for (User user : userList) {
			lastVisitedLocations.put(user.getUserId().toString(), getUserLocation(user).location);
		}
		return lastVisitedLocations;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
