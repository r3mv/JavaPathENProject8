package tourGuide.tracker;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.service.TourGuideService;
import tourGuide.user.User;

public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);
	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	private final ExecutorService trackingExecutor = Executors.newFixedThreadPool(1000);
	private final TourGuideService tourGuideService;
	private boolean stop = false;


	public Tracker(TourGuideService tourGuideService) {
		this.tourGuideService = tourGuideService;
		executorService.submit(this);
	}

	public void startTracking() {

	}


	/**
	 * Assures to shut down the Tracker thread
	 */
	public void stopTracking() {
		stop = true;
		executorService.shutdownNow();
	}

	@Override
	public void run() {
		StopWatch stopWatch = new StopWatch();

		while(true) {
			if (Thread.currentThread().isInterrupted() || stop) {
				logger.debug("Tracker stopping");
				break;
			}

			List<User> users = tourGuideService.getAllUsers();

			while (!stop) {
				logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
				stopWatch.start();

				CompletableFuture[] futures = users.stream()
						.map(user -> CompletableFuture.supplyAsync(() -> tourGuideService.getGpsUtil().getUserLocation(user.getUserId()), trackingExecutor)
								.thenApplyAsync(location -> {
									user.addToVisitedLocations(location);
									return location;
								}, trackingExecutor)
								.thenAcceptAsync(location -> tourGuideService.getRewardsService().calculateRewards(user), trackingExecutor)
						)
						.toArray(CompletableFuture[]::new);
				LoggerFactory.getLogger(Tracker.class).debug("{} completable futures launched", futures.length);
				CompletableFuture<Void> track = CompletableFuture.allOf(futures);
				track.join();
				stopWatch.stop();
				logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
				stopWatch.reset();
				try {
					logger.debug("Tracker sleeping");
					TimeUnit.SECONDS.sleep(trackingPollingInterval);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
