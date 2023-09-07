package tourGuide;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
	@RequestMapping("/")
	public String index() {
		return "Greetings from TourGuide!";
	}

	/**
	 * Get location of a user by his userName
	 * 
	 * @param userName - String
	 * @return Json object with location of a specific user
	 */
	@RequestMapping("/getLocation")
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
	@RequestMapping("/getNearbyAttractions")
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
	@RequestMapping("/getRewards")
	public String getRewards(@RequestParam String userName) {
		lOGGER.debug("Getting rewards for username : {}", userName);
		return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
	}

	/**
	 * Get current locations of all users
	 * 
	 * @return Json object that contains all users current locations
	 */
	@RequestMapping("/getAllCurrentLocations")
	public String getAllCurrentLocations() {
		// TODO: Get a list of every user's most recent location as JSON
		// - Note: does not use gpsUtil to query for their current location,
		// but rather gathers the user's current location from their stored location
		// history.
		//
		// Return object should be the just a JSON mapping of userId to Locations
		// similar to:
		// {
		// "019b04a9-067a-4c76-8817-ee75088c3822":
		// {"longitude":-48.188821,"latitude":74.84371}
		// ...
		// }
		lOGGER.debug("Getting all current locations");
		return JsonStream.serialize(tourGuideService.getAllCurrentLocations());
	}

	/**
	 * Get tripDeals for a user by his userName
	 * 
	 * @param userName - String
	 * @return Json object that contains tripDeals for a specific user
	 */
	@RequestMapping("/getTripDeals")
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