package tourGuide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import tourGuide.service.GpsUtilService;
import tourGuide.service.RewardsService;

/**
 * Configuration class of TourGuide application
 *
 */
@Configuration
public class TourGuideModule {
	
	@Bean
	public GpsUtilService getGpsUtilService() {
		return new GpsUtilService();
	}
	
	@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getGpsUtilService(), getRewardCentral());
	}
	
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
