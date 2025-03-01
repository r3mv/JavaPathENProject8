package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {

	private Logger LOG = LoggerFactory.getLogger(RewardsService.class);
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}
	}

	public void calculateRewards(List<User> users) {
//		Stream.of(users, gpsUtil.getAttractions())
//				.forEach((u, a) -> {});
//		List<Attraction> attractions = gpsUtil.getAttractions();
//
//		users.parallelStream().forEach(
//				user -> {
//					List<VisitedLocation> userLocations = user.getVisitedLocations();
//					userLocations.parallelStream().forEach(visitedLocation -> {
//						attractions.parallelStream()
//								.filter(attraction -> user.getUserRewards()
//										.stream()
//										.noneMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName)))
//								.filter(attraction -> nearAttraction(visitedLocation, attraction))
//								.forEach(attraction -> {
//									LoggerFactory.getLogger(RewardsService.class).debug("Computing rewards attraction {} for user {} from thread {}",attraction.attractionName,user.getUserName(), Thread.currentThread().getName());
//									user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//								});
//					});
//				}
//		);


	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	int counterOfGetDistanceUsage = 0;

	void resetCounterOfGetDistanceUsage() {
		counterOfGetDistanceUsage = 0;
	}

	int getCounterOfGetDistanceUsage() {
		return counterOfGetDistanceUsage;
	}
	public double getDistance(Location loc1, Location loc2) {
		counterOfGetDistanceUsage++;
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

}
