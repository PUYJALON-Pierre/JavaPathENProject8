package tourGuide;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tripPricer.Provider;

/**
 * Controller class for TourGuide Application
 *
 *
 */
@RestController
public class TourGuideController {

	final static Logger lOGGER = LogManager.getLogger(TourGuideController.class);

	@Autowired
	TourGuideService tourGuideService;

	/**
	 * Get TourGuide welcome message
	 * 
	 * @return Welcome message - String
	 */
	@GetMapping("/")
	public String index() {
		return "Greetings from TourGuide!";
	}

	/**
	 * Get location of a user by his userName
	 * 
	 * @param userName - String
	 * @return Json object with location of a specific user
	 */
	@GetMapping("/getLocation")
	public String getLocation(@RequestParam String userName) {
		lOGGER.debug("Getting location of username : {}", userName);
		VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
	}

	/**
	 * Get 5 nearest attractions from a specific user by his userName
	 * 
	 * @param userName - String
	 * @return Json object that contain informations of the five nearest attractions
	 *         - List of NearByAttractionsDTO
	 */
	@GetMapping("/getNearbyAttractions")
	public String getNearbyAttractions(@RequestParam String userName) {
		lOGGER.debug("Getting 5 nearest attractions for username : {}", userName);
		VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
	}

	/**
	 * Get rewards of a user by his userName
	 * 
	 * @param userName - String
	 * @return Json object that contains rewards of a specific user
	 */
	@GetMapping("/getRewards")
	public String getRewards(@RequestParam String userName) {
		lOGGER.debug("Getting rewards for username : {}", userName);
		return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
	}

	/**
	 * Get current locations of all users
	 * 
	 * @return Json object that contains all users current locations
	 */
	@GetMapping("/getAllCurrentLocations")
	public String getAllCurrentLocations() {
		lOGGER.debug("Getting all current locations");
		return JsonStream.serialize(tourGuideService.getAllCurrentLocations());
	}

	/**
	 * Get tripDeals for a user by his userName
	 * 
	 * @param userName - String
	 * @return Json object that contains tripDeals for a specific user
	 */
	@GetMapping("/getTripDeals")
	public String getTripDeals(@RequestParam String userName) {
		lOGGER.debug("Getting trip deals for username : {}", userName);
		List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
		return JsonStream.serialize(providers);
	}

	/**
	 * Get a User from his userName
	 * 
	 * @param userName - String
	 * @return a specific user - User
	 */
	private User getUser(String userName) {
		lOGGER.debug("Getting User with username : {}", userName);
		return tourGuideService.getUser(userName);
	}

}