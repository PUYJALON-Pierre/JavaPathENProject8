package tourGuide.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;


@Service
public class GpsUtilService {

	private GpsUtil gpsUtil;

	public GpsUtilService() {
		this.gpsUtil = new GpsUtil();
	}

	private final ExecutorService executorService = Executors.newFixedThreadPool(1200);

	public List<Attraction> getListOfAttractions() {
		return gpsUtil.getAttractions();
	}

    public VisitedLocation getUserLocation(UUID userId){
        try{
            return gpsUtil.getUserLocation(userId);
        }catch(NumberFormatException numberFormatException){
            numberFormatException.printStackTrace();
        }
        return null;
    }

}
