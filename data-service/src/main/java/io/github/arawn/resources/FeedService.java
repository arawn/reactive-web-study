package io.github.arawn.resources;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Service
public class FeedService {

    static String[] words = new String[]{ "magna. Praesent interdum ligula eu", "feugiat placerat velit. Quisque varius.", "parturient montes, nascetur ridiculus mus.", "Nulla interdum. Curabitur dictum. Phasellus in felis. Nulla tempor augue", "Morbi non sapien molestie orci tincidunt adipiscing. Mauris", "dui. Cum sociis natoque penatibus et magnis dis parturient montes,", "Nunc mauris elit, dictum eu, eleifend nec,", "elit, dictum eu, eleifend", "pede et risus. Quisque", "Donec non", "Praesent interdum ligula eu enim. Etiam", "felis. Donec tempor, est ac mattis semper, dui lectus rutrum", "at arcu. Vestibulum", "et", "tristique pharetra. Quisque ac libero nec ligula consectetuer rhoncus. Nullam", "eu,", "Vivamus nisi. Mauris nulla. Integer urna. Vivamus molestie", "Donec sollicitudin adipiscing ligula. Aenean gravida nunc", "dictum. Phasellus in felis. Nulla tempor", "ligula. Aenean euismod mauris eu elit. Nulla", "dapibus name, blandit at, nisi. Cum sociis natoque penatibus et", "name magna et ipsum cursus vestibulum.", "ipsum primis in faucibus orci luctus et", "fermentum arcu.", "lacinia vitae, sodales at, velit. Pellentesque ultricies dignissim", "sit amet metus. Aliquam erat volutpat.", "luctus ut, pellentesque eget, dictum placerat, augue. Sed molestie. Sed", "luctus sit amet, faucibus ut,", "diam", "tortor, dictum eu, placerat eget, venenatis a,", "blandit congue. In scelerisque", "Ut sagittis lobortis mauris. Suspendisse", "Suspendisse commodo tincidunt nibh.", "odio, auctor vitae, aliquet nec, imperdiet nec, leo. Morbi neque", "et magnis dis parturient montes,", "interdum. Nunc", "nostra, per", "tortor", "a tortor. Nunc commodo auctor velit. Aliquam nisl. Nulla eu", "in, tempus eu, ligula. Aenean euismod", "et risus. Quisque libero lacus, varius", "justo sit amet nulla. Donec non justo.", "ridiculus mus. Aenean eget magna.", "a", "risus a ultricies adipiscing, enim mi tempor lorem, eget mollis", "nec ante. Maecenas mi felis, adipiscing fringilla, porttitor vulputate, posuere", "enim.", "Quisque tincidunt pede ac", "non magna. Nam ligula elit, pretium", "justo. Praesent luctus. Curabitur egestas nunc sed", "malesuada fames ac", "sapien. Nunc pulvinar arcu et pede. Nunc", "luctus aliquet odio. Etiam ligula", "sit amet risus. Donec egestas. Aliquam nec enim. Nunc ut", "consequat,", "Donec feugiat metus sit amet ante. Vivamus non", "augue scelerisque mollis. Phasellus libero mauris, aliquam eu, accumsan sed,", "dolor", "magna a tortor. Nunc commodo auctor", "diam. Pellentesque habitant morbi tristique senectus et", "metus. Aenean sed pede", "magna tellus faucibus leo, in lobortis tellus justo sit amet", "molestie in, tempus eu, ligula. Aenean euismod", "et malesuada fames ac turpis egestas. Aliquam fringilla", "Nullam scelerisque", "Nunc commodo auctor velit. Aliquam", "dapibus ligula.", "lorem tristique aliquet. Phasellus", "adipiscing elit. Aliquam auctor, velit eget laoreet posuere,", "mollis vitae, posuere at, velit. Cras lorem", "nulla. Cras", "magnis dis parturient montes, nascetur ridiculus mus.", "In lorem. Donec elementum, lorem ut aliquam iaculis,", "ipsum sodales purus, in molestie", "Pellentesque habitant morbi", "massa. Mauris vestibulum,", "tempor arcu. Vestibulum ut eros non enim commodo", "eu tellus.", "montes, nascetur ridiculus mus.", "sit amet", "eu augue porttitor interdum. Sed", "eget metus. In nec orci. Donec nibh. Quisque nonummy", "non, feugiat nec, diam. Duis mi enim, condimentum eget,", "sociis natoque penatibus et", "Fusce aliquam, enim nec tempus scelerisque, lorem ipsum sodales", "justo eu arcu. Morbi sit amet", "ligula tortor, dictum eu, placerat eget,", "Quisque varius. Nam porttitor scelerisque neque. Nullam nisl. Maecenas malesuada", "elit. Nulla facilisi. Sed neque. Sed", "malesuada vel, venenatis vel, faucibus name, libero. Donec", "et malesuada", "nisi nibh", "rutrum. Fusce", "tellus eu", "eu,", "enim commodo hendrerit. Donec porttitor tellus non", "lacus. Quisque imperdiet, erat", "sapien molestie orci tincidunt adipiscing. Mauris molestie pharetra", "velit. Quisque varius. Nam porttitor", "lacinia at, iaculis quis, pede. Praesent eu dui. Cum" };


    public Mono<Feed> getFeeds(String username, Optional<ServiceProcessing> processing) {
        return Mono.fromCallable(() -> {
            Feed feed = new Feed();
            feed.setContent(words[(int) (Math.random() * 99 + 0)]);

            return feed;
        }).subscribeOn(Schedulers.parallel())
          .delayElementMillis(processing.orElse(ServiceProcessing.MEDIUM).getDelayMillis());
    }

    public Mono<Long> getFeedCount(String username, Optional<ServiceProcessing> processing) {
        return Mono.fromCallable(() -> {
            return (long) (Math.random() * 99 + 0);
        }).subscribeOn(Schedulers.parallel())
          .delayElementMillis(processing.orElse(ServiceProcessing.MEDIUM).getDelayMillis());
    }


    public static class Feed {

        String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

}
