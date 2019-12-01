import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class ServerModelTest {
    private ServerModel model;

    @Before
    public void setUp() {
        // We initialize a fresh ServerModel for each test
        model = new ServerModel();
    }

    /* Here is an example test that checks the functionality of your changeNickname error handling.
     * Each line has commentary directly above it which you can use as a framework for the remainder
     * of your tests.
     */
    @Test
    public void testInvalidNickname() {
        // A user must be registered before their nickname can be changed, so we first register a
        // user with an arbitrarily chosen id of 0.
        model.registerUser(0);

        // We manually create a Command that appropriately tests the case we are checking.
        // In this case, we create a NicknameCommand whose new Nickname is invalid.
        Command command = new NicknameCommand(0, "User0", "!nv@l!d!");

        // We manually create the expected Broadcast using the Broadcast factory methods.
        // In this case, we create an error Broadcast with our command and an INVALID_NAME error.
        Broadcast expected = Broadcast.error(command, ServerError.INVALID_NAME);

        // We then get the actual Broadcast returned by the method we are trying to test.
        // In this case, we use the updateServerModel method of the NicknameCommand.
        Broadcast actual = command.updateServerModel(model);

        // The first assertEquals call tests whether the method returns the appropriate broacast.
        assertEquals("Broadcast", expected, actual);

        // We also want to test whether the state has been correctly changed.
        // In this case, the state that would be affected is the user's Collection.
        Collection<String> users = model.getRegisteredUsers();

        // We now check to see if our command updated the state appropriately.
        // In this case, we first ensure that no additional users have been added.
        assertEquals("Number of registered users", 1, users.size());

        // We then check if the username was updated to an invalid value(it should not have been).
        assertTrue("Old nickname still registered", users.contains("User0"));

        // Finally, we check that the id 0 is still associated with the old, unchanged nickname.
        assertEquals("User with id 0 nickname unchanged", "User0", model.getNickname(0));
    }
    
    @Test
    public void createPrivateChannel() {
        // this command will create a channel called "java" with "User0" (with id = 0) as the owner
    	model.registerUser(0);
    	
        Command create = new CreateCommand(0, "User0", "java", true);
        Broadcast expected = Broadcast.okay(create, Collections.singleton("User0"));
        
        assertEquals("broadcast", expected, create.updateServerModel(model));

        assertTrue("channel exists", model.getChannels().contains("java"));
        assertTrue("channel has creator", model.getUsersInChannel("java").contains("User0"));
        assertEquals("channel has owner", "User0", model.getOwner("java"));
    }
    
    @Test 
    public void joinPrivateChannel() {
    	model.registerUser(0);
    	model.registerUser(1);
    	
    	Command create = new CreateCommand(0, "User0", "java", true);
    	create.updateServerModel(model);
    	
    	Command invite = new JoinCommand(1, "User1", "java");
    	Broadcast expected = Broadcast.error(invite, ServerError.JOIN_PRIVATE_CHANNEL);
    	
    	assertEquals("broadcast", expected, invite.updateServerModel(model));
    	assertTrue("User0 in channel", model.getUsersInChannel("java").contains("User0"));
    	assertFalse("User1 in channel", model.getUsersInChannel("java").contains("User1"));
    }
    
    @Test
    public void inviteToPrivateChannel() {
    	model.registerUser(0);
    	model.registerUser(1);
    	model.registerUser(2);
    	
    	Command create = new CreateCommand(0, "User0", "java", true);
    	create.updateServerModel(model);
    	
    	Command invite = new InviteCommand(0, "User0", "java", "User1");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        
    	Broadcast expected = Broadcast.names(invite, recipients, "User0");
    	assertEquals("broadcast", expected, invite.updateServerModel(model));

        assertTrue("User0 in channel", model.getUsersInChannel("java").contains("User0"));
        assertTrue("User1 in channel", model.getUsersInChannel("java").contains("User1"));
        assertEquals("num. users in channel", 2, model.getUsersInChannel("java").size());
        
        Command invite1 = new InviteCommand(1, "User1", "java", "User2");
        
        expected = Broadcast.error(invite1, ServerError.USER_NOT_OWNER);
        
        assertEquals("broadcast faulty", expected, invite1.updateServerModel(model));
        assertTrue("User0 in channel", model.getUsersInChannel("java").contains("User0"));
        assertTrue("User1 in channel", model.getUsersInChannel("java").contains("User1"));
        assertFalse("User2 in channel", model.getUsersInChannel("java").contains("User2"));
        
    }
    
    @Test
    public void ownerLeavePublicChannel() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command leave = new LeaveCommand(0, "User0", "java");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(leave, recipients);
        assertEquals("broadcast", expected, leave.updateServerModel(model));
        assertEquals("channel doesn't exist", 0, model.getUsersInChannel("java").size());
    }
    
    @Test
    public void ownerLeavePrivateChannel() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);
        Command invite = new InviteCommand(0, "User0", "java", "User1");
        invite.updateServerModel(model);

        Command leave = new LeaveCommand(0, "User0", "java");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(leave, recipients);
        assertEquals("broadcast", expected, leave.updateServerModel(model));
        assertEquals("channel doesn't exist", 0, model.getUsersInChannel("java").size());
    }
    
    @Test
    public void nonOwnerKickPrivateChannel() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);
        Command invite = new InviteCommand(0, "User0", "java", "User1");
        invite.updateServerModel(model);
        
        Command kick = new KickCommand(1, "User1", "java", "User0");
        Broadcast expected = Broadcast.error(kick, ServerError.USER_NOT_OWNER);
        
        assertEquals("broadcast", expected, kick.updateServerModel(model));
        assertEquals("channel is the same", 2, model.getUsersInChannel("java").size());
    }
    
    @Test
    public void ownerDeregister() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);
        
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        
        Broadcast expected = Broadcast.disconnected("User0", recipients);
        
        assertEquals("deregister owner", expected, model.deregisterUser(0));
        assertFalse("Channel is gone", model.getChannels().contains("java"));
    }
    

    /*
     * Your TAs will be manually grading the tests you write in this file.
     * Don't forget to test both the public methods you have added to your
     * ServerModel class as well as the behavior of the server in different
     * scenarios.
     * You might find it helpful to take a look at the tests we have already
     * provided you with in ChannelsMessagesTest, ConnectionNicknamesTest,
     * and InviteOnlyTest.
     */

    // TODO: Add your own tests here...
}
