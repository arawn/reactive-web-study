package io.github.arawn.resources;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getUser(String username) {
        return Mono.fromCallable(() -> userRepository.findByUsername(username))
                   .subscribeOn(Schedulers.parallel())
                   .delayElementMillis(ServiceProcessing.FAST.getDelayMillis());
    }


    public static class User {

        String username;
        String name;

        public User(String username, String name) {
            this.username = username;
            this.name = name;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @Repository
    public static class UserRepository {

        List<User> users = new ArrayList<>();

        User findByUsername(String username) {
            return users.stream().filter(user -> Objects.equals(user.getUsername(), username)).findFirst().orElseThrow(() -> new RuntimeException(username + "을 찾을 수 없습니다."));
        }

        List<User> findAll() {
            return users;
        }

    }

    @Component
    public static class UserDataInitialize implements CommandLineRunner {

        final String[] usernames = new String[]{ "joan", "fay", "byron", "adrienne", "veda", "darius", "uta", "sharon", "jordan", "basia", "murphy", "elton", "jacqueline", "dieter", "lydia", "macey", "karyn", "hillary", "jolene", "shelby", "india", "eric", "jamalia", "jane", "kai", "sydnee", "florence", "nichole", "whoopi", "kasper", "marny", "akeem", "abel", "tara", "isadora", "phillip", "odysseus", "lareina", "anika", "adrienne", "ivory", "georgia", "faith", "quintessa", "asher", "noelani", "ciara", "leah", "ishmael", "nita", "amaya", "martha", "baker", "yen", "inga", "celeste", "aspen", "prescott", "jameson", "mackensie", "aiko", "aubrey", "tasha", "sybil", "wanda", "vernon", "keane", "iris", "duncan", "hadley", "ariana", "grady", "dante", "alice", "wade", "brennan", "karyn", "paul", "jeanette", "colby", "quin", "jasper", "jessamine", "kuame", "simon", "demetria", "levi", "jana", "wade", "brent", "gil", "declan", "octavia", "abbot", "audrey", "kai", "jennifer", "glenna", "peter", "tarik" };
        final String[] names = new String[]{ "Keefe R. Gardner", "Micah O. Mullen", "Honorato U. Farrell", "Keely N. Bell", "Mohammad D. Trujillo", "Oliver V. Mays", "Robin V. Hurst", "Willow K. Mayer", "Isaiah C. Kramer", "Len A. Horton", "Eden I. Solis", "Ira U. Bradshaw", "Nathaniel M. Crosby", "May R. Mclaughlin", "Freya G. Rice", "Jordan D. Dale", "Odysseus K. Tillman", "Cally H. Ball", "Xerxes I. Robbins", "Britanni A. Lindsey", "Price N. Eaton", "Ashely P. Ramirez", "Zachary O. Weeks", "Declan T. Macdonald", "Xena N. Carroll", "Wayne S. Moore", "Stacey P. Berry", "Conan J. Osborne", "Ina H. Small", "Raymond K. Rollins", "Maggie W. Farrell", "Devin U. Leon", "Charissa Z. Mills", "Latifah T. Carson", "Ann I. Valenzuela", "Eve P. Benton", "Lila D. Hays", "Oliver E. Duncan", "Allegra M. Avila", "Yeo D. Collins", "Martin T. Boyle", "Naomi Z. Patel", "Dana G. Logan", "Lael I. Russo", "Doris S. Puckett", "Kimberley N. Crane", "Jackson Y. Boyd", "Shellie M. Elliott", "Ocean D. Phelps", "Sarah T. Macdonald", "Ori O. Harvey", "Lydia J. Lyons", "Elijah B. Sawyer", "Maite Q. Leblanc", "Mikayla L. Chase", "Beau I. Wright", "Rachel L. Blackwell", "Peter U. Hatfield", "Xaviera G. Bray", "Avram S. Reeves", "Neve O. Meyer", "Jorden Q. Newman", "Nissim M. Morrow", "Nita Y. Rojas", "Constance M. Koch", "Davis Y. Garcia", "Brett Q. Barnes", "Chaim J. Bauer", "Jelani A. Spence", "Iris P. Ramsey", "Bert U. Lindsey", "Timon Q. Schmidt", "Holly J. Stone", "Jerome Y. Ferrell", "Wing P. Gutierrez", "Freya A. Carroll", "Molly G. Lynch", "Samuel X. Frye", "Kelly L. Howell", "Maxwell E. Goff", "Uma D. Good", "Shellie R. Walters", "Harlan D. Bailey", "Guy Y. Ballard", "Cullen S. Watts", "Xavier A. Ochoa", "Yoshio N. Faulkner", "Yuli Q. Hanson", "Dakota A. Odom", "Preston S. Anderson", "Anastasia X. Dodson", "Britanney P. Keith", "Madaline I. Roth", "Phyllis V. Moody", "Zenia J. Rich", "Gray E. Chapman", "Veronica B. Scott", "Hop X. Frederick", "Hadley E. Navarro", "Thomas J. Horne" };


        UserRepository userRepository;

        public UserDataInitialize(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        public void run(String... args) throws Exception {
            for(int idx = 0; idx < 100; idx++) {
                userRepository.users.add(new User(usernames[idx], names[idx]));
            }
        }

    }

}