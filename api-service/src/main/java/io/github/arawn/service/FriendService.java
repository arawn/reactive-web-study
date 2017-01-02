package io.github.arawn.service;

public class FriendService {

    private static final String[] usernames = new String[]{ "Keefe R. Gardner", "Micah O. Mullen", "Honorato U. Farrell", "Keely N. Bell", "Mohammad D. Trujillo", "Oliver V. Mays", "Robin V. Hurst", "Willow K. Mayer", "Isaiah C. Kramer", "Len A. Horton", "Eden I. Solis", "Ira U. Bradshaw", "Nathaniel M. Crosby", "May R. Mclaughlin", "Freya G. Rice", "Jordan D. Dale", "Odysseus K. Tillman", "Cally H. Ball", "Xerxes I. Robbins", "Britanni A. Lindsey", "Price N. Eaton", "Ashely P. Ramirez", "Zachary O. Weeks", "Declan T. Macdonald", "Xena N. Carroll", "Wayne S. Moore", "Stacey P. Berry", "Conan J. Osborne", "Ina H. Small", "Raymond K. Rollins", "Maggie W. Farrell", "Devin U. Leon", "Charissa Z. Mills", "Latifah T. Carson", "Ann I. Valenzuela", "Eve P. Benton", "Lila D. Hays", "Oliver E. Duncan", "Allegra M. Avila", "Yeo D. Collins", "Martin T. Boyle", "Naomi Z. Patel", "Dana G. Logan", "Lael I. Russo", "Doris S. Puckett", "Kimberley N. Crane", "Jackson Y. Boyd", "Shellie M. Elliott", "Ocean D. Phelps", "Sarah T. Macdonald", "Ori O. Harvey", "Lydia J. Lyons", "Elijah B. Sawyer", "Maite Q. Leblanc", "Mikayla L. Chase", "Beau I. Wright", "Rachel L. Blackwell", "Peter U. Hatfield", "Xaviera G. Bray", "Avram S. Reeves", "Neve O. Meyer", "Jorden Q. Newman", "Nissim M. Morrow", "Nita Y. Rojas", "Constance M. Koch", "Davis Y. Garcia", "Brett Q. Barnes", "Chaim J. Bauer", "Jelani A. Spence", "Iris P. Ramsey", "Bert U. Lindsey", "Timon Q. Schmidt", "Holly J. Stone", "Jerome Y. Ferrell", "Wing P. Gutierrez", "Freya A. Carroll", "Molly G. Lynch", "Samuel X. Frye", "Kelly L. Howell", "Maxwell E. Goff", "Uma D. Good", "Shellie R. Walters", "Harlan D. Bailey", "Guy Y. Ballard", "Cullen S. Watts", "Xavier A. Ochoa", "Yoshio N. Faulkner", "Yuli Q. Hanson", "Dakota A. Odom", "Preston S. Anderson", "Anastasia X. Dodson", "Britanney P. Keith", "Madaline I. Roth", "Phyllis V. Moody", "Zenia J. Rich", "Gray E. Chapman", "Veronica B. Scott", "Hop X. Frederick", "Hadley E. Navarro", "Thomas J. Horne" };
    final AwkwardChaosMonkey chaosMonkey = new AwkwardChaosMonkey();

    public FriendRequestNotify getFriendRequestNotify() {
        if (chaosMonkey.doItNow()) {
            throw new ServiceOperationException();
        }

        NetworkLatency.fast();

        FriendRequestNotify friendRequestNotify = new FriendRequestNotify();
        friendRequestNotify.setUsername(usernames[(int) (Math.random() * 99 + 0)]);

        return friendRequestNotify;
    }


    public static class FriendRequestNotify {

        String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

    }

}
