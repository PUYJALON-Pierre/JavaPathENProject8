package tourGuide.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import tourGuide.user.User;

@Service
public class GpsUtilService {

	private GpsUtil gpsUtil;

	public GpsUtilService() {
		this.gpsUtil = new GpsUtil();
	}

	private ExecutorService executor = Executors.newFixedThreadPool(1200);

	public List<Attraction> getAttractions() {
		return gpsUtil.getAttractions();
	}

	public void submitLocation(User user, TourGuideService tourGuideService) {

		CompletableFuture.supplyAsync(() -> {
			return gpsUtil.getUserLocation(user.getUserId());
		}, executor).thenAccept(visitedLocation -> {
			try {
				tourGuideService.finalizeLocation(user, visitedLocation);
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

}
