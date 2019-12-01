import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * These tests are provided for testing the server's handling of channels and messages. You can and
 * should use these tests as a model for your own testing, but write your tests in
 * ServerModelTest.java.
 *
 * Note that "assert" commands used for testing can have a String as their first argument, denoting
 * what the "assert" command is testing.
 * 
 * Remember to check http://junit.sourceforge.net/javadoc/org/junit/Assert.html for assert method
 * documention!
 */
public class ChannelsMessagesTest {
    private ServerModel model;

    // Before each test, we initialize model to be a new ServerModel (with all new, empty state)
    @Before
    public void setUp() {
        model = new ServerModel();
    }

    @Test
    public void testCreateNewChannel() {
        model.registerUser(0);
        Command create = new CreateCommand(0, "User0", "java", false);
        Broadcast expected = Broadcast.okay(create, Collections.singleton("User0"));
        assertEquals("broadcast", expected, create.updateServerModel(model));

        assertTrue("channel exists", model.getChannels().contains("java"));
        assertTrue("channel has creator", model.getUsersInChannel("java").contains("User0"));
        assertEquals("channel has owner", "User0", model.getOwner("java"));
    }

    @Test
    public void testJoinChannelExistsNotMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.names(join, recipients, "User0");
        assertEquals("broadcast", expected, join.updateServerModel(model));

        assertTrue("User0 in channel", model.getUsersInChannel("java").contains("User0"));
        assertTrue("User1 in channel", model.getUsersInChannel("java").contains("User1"));
        assertEquals("num. users in channel", 2, model.getUsersInChannel("java").size());
    }

    @Test
    public void testNickBroadcastsToChannelWhereMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);
        Command nick = new NicknameCommand(0, "User0", "Duke");

        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("Duke");
        Broadcast expected = Broadcast.okay(nick, recipients);
        assertEquals("broadcast", expected, nick.updateServerModel(model));

        assertFalse("old nick not in channel", model.getUsersInChannel("java").contains("User0"));
        assertTrue("new nick is in channel", model.getUsersInChannel("java").contains("Duke"));
        assertTrue("unaffected user still in channel", model.getUsersInChannel("java").contains("User1"));
    }

    @Test
    public void testLeaveChannelExistsMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command leave = new LeaveCommand(1, "User1", "java");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(leave, recipients);
        assertEquals("broadcast", expected, leave.updateServerModel(model));

        assertTrue("User0 in channel", model.getUsersInChannel("java").contains("User0"));
        assertFalse("User1 not in channel", model.getUsersInChannel("java").contains("User1"));
        assertEquals("num. users in channel", 1, model.getUsersInChannel("java").size());
    }

    @Test
    public void testDeregisterSendsDisconnectedWhereMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Broadcast expected = Broadcast.disconnected("User1", Collections.singleton("User0"));
        assertEquals("broadcast", expected, model.deregisterUser(1));

        assertTrue("channel still exists", model.getChannels().contains("java"));
        assertEquals("num. users in channel", 1, model.getUsersInChannel("java").size());
        assertTrue("unaffected user still in channel", model.getUsersInChannel("java").contains("User0"));
    }

    @Test
    public void testMesgChannelExistsMember() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command mesg = new MessageCommand(0, "User0", "java", "hey whats up hello");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(mesg, recipients);
        assertEquals("broadcast", expected, mesg.updateServerModel(model));
    }

}

