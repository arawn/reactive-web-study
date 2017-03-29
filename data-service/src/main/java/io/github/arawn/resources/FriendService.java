package io.github.arawn.resources;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
public class FriendService {

    UserService.UserRepository userRepository;

    public FriendService(UserService.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<FriendRecommendation> getFriendRecommendation(String username, Optional<ServiceProcessing> processing) {
        return Mono.fromCallable(() -> {
            UserService.User random = userRepository.users.get((int) (Math.random() * 99 + 0));

            FriendRecommendation friendRecommendation = new FriendRecommendation();
            friendRecommendation.setName(random.getName());

            return friendRecommendation;
        }).subscribeOn(Schedulers.parallel())
          .delayElementMillis(processing.orElse(ServiceProcessing.VERYSLOW).getDelayMillis());
    }

    public Mono<Long> getFriendCount(String username, Optional<ServiceProcessing> processing) {
        return Mono.fromCallable(() -> {
            return (long) (Math.random() * 99 + 0);
        }).subscribeOn(Schedulers.parallel())
          .delayElementMillis(processing.orElse(ServiceProcessing.MEDIUM).getDelayMillis());
    }


    public static class FriendRecommendation {

        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}