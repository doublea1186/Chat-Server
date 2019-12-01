import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Represents a command string sent from a client to the server, after it has been parsed into a
 * more convenient form. The {@code Command} abstract class has a concrete subclass corresponding to
 * each of the possible commands that can be issued by a client. The protocol specification contains
 * more information about the expected behavior of various commands.
 */
public abstract class Command {

    /**
     * The server-assigned ID of the user who sent the {@code Command}.
     */
    private int senderId;

    /**
     * The current nickname in use by the sender of the {@code command}.
     */
    private String sender;

    Command(int senderId, String sender) {
        this.senderId = senderId;
        this.sender = sender;
    }

    /**
     * Gets the user ID of the client who issued the {@code Command}.
     *
     * @return The user ID of the client who issued this command
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * Gets the nickname of the client who issued the {@code Command}.
     *
     * @return The nickname of the client who issued this command
     */
    public String getSender() {
        return sender;
    }

    /**
     * Processes the command and updates the server model accordingly.
     *
     * @param model An instance of the {@link ServerModelApi} class which represents the current
     *              state of the server.
     * @return A {@link Broadcast} object, informing clients about changes resulting from the
     *      command.
     */
    public abstract Broadcast updateServerModel(ServerModel model);

    /**
     * Returns {@code true} if two {@code Command}s are equal; that is, they produce the same string
     * representation.
     *
     * @param o the object to compare with {@code this} for equality
     * @return true iff both objects are non-null and equal to each other
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Command)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }
}


//==============================================================================
// Command subclasses
//==============================================================================

/**
 * Represents a {@link Command} issued by a client to change his or her nickname.
 */
class NicknameCommand extends Command {
    private String newNickname;

    public NicknameCommand(int senderId, String sender, String newNickname) {
        super(senderId, sender);
        this.newNickname = newNickname;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle nickname command
        return model.changeNickname(this);
    }

    public String getNewNickname() {
        return newNickname;
    }

    @Override
    public String toString() {
        return String.format(":%s NICK %s", getSender(), newNickname);
    }
}

/**
 * Represents a {@link Command} issued by a client to create a new channel.
 */
class CreateCommand extends Command {
    private String channel;
    private boolean inviteOnly;

    public CreateCommand(int senderId, String sender, String channel, boolean inviteOnly) {
        super(senderId, sender);
        this.channel = channel;
        this.inviteOnly = inviteOnly;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle create command
    	ServerError response = model.createChannel(this.channel, this.getSender(), this.inviteOnly);
    	
    	if (response == ServerError.OKAY) {
        	
    		return Broadcast.okay(this, Collections.singleton(this.getSender()));
    	} else {
    		
    		return Broadcast.error(this, response);
    	}
    	
    }

    public String getChannel() {
        return channel;
    }

    public boolean isInviteOnly() {
        return inviteOnly;
    }

    @Override
    public String toString() {
        int flag = inviteOnly ? 1 : 0;
        return String.format(":%s CREATE %s %d", getSender(), channel, flag);
    }
}

/**
 * Represents a {@link Command} issued by a client to join an existing channel.
 * All users in the channel (including the new one) should be notified about when 
 * a "join" occurs.
 */
class JoinCommand extends Command {
    private String channel;

    public JoinCommand(int senderId, String sender, String channel) {
        super(senderId, sender);
        this.channel = channel;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle join command
    	ServerError response = model.addUserToChannel(this.getSender(), this.channel);
    	if (response == ServerError.OKAY) {
    		
    		return Broadcast.names(this, model.getUsersInChannel(this.channel), model.getOwner(this.channel));
    	} else {
    		
    		return Broadcast.error(this, response);
    	}
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s JOIN %s", getSender(), channel);
    }
}

/**
 * Represents a {@link Command} issued by a client to send a message to all other clients in the
 * channel.
 */
class MessageCommand extends Command {
    private String channel;
    private String message;

    public MessageCommand(int senderId, String sender, String channel, String message) {
        super(senderId, sender);
        this.channel = channel;
        this.message = message;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle message command
    	if (!model.getChannels().contains(this.channel)) {
    		
    		return Broadcast.error(this, ServerError.NO_SUCH_CHANNEL);
    	} else if (!model.getUsersInChannel(this.channel).contains(this.getSender())) {
    		
    		return Broadcast.error(this, ServerError.USER_NOT_IN_CHANNEL);
    	}
    	
        return Broadcast.okay(this, model.getUsersInChannel(this.channel));
    }
    
    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s MESG %s :%s", getSender(), channel, message);
    }
}

/**
 * Represents a {@link Command} issued by a client to leave a channel.
 */
class LeaveCommand extends Command {
    private String channel;

    public LeaveCommand(int senderId, String sender, String channel) {
        super(senderId, sender);
        this.channel = channel;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle leave command
    	Collection<String> previousUsers = model.getUsersInChannel(this.getChannel());
    	
    	ServerError response = 
    			model.removeUserFromChannel(this.getSender(), this.getChannel(), model.getOwner(this.getChannel()));
    	
    	if (response == ServerError.OKAY) {
    		
    		return Broadcast.okay(this, previousUsers);
    	} else {
    		
    		return Broadcast.error(this, response);
    	}
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return String.format(":%s LEAVE %s", getSender(), channel);
    }
}

/**
 * Represents a {@link Command} issued by a client to add another client to an invite-only channel
 * owned by the sender.
 */
class InviteCommand extends Command {
    private String channel;
    private String userToInvite;

    public InviteCommand(int senderId, String sender, String channel, String userToInvite) {
        super(senderId, sender);
        this.channel = channel;
        this.userToInvite = userToInvite;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle invite command	
    	ServerError response = 
    			model.addUserToPrivateChannel(this.getUserToInvite(), this.getChannel(), this.getSender());
    	if (response == ServerError.OKAY) {
    		
    		return Broadcast.names(this, model.getUsersInChannel(this.getChannel()), model.getOwner(this.getChannel()));    			
    	} else {
    		
    		return Broadcast.error(this, response);
    	}
    }

    public String getChannel() {
        return channel;
    }

    public String getUserToInvite() {
        return userToInvite;
    }

    @Override
    public String toString() {
        return String.format(":%s INVITE %s %s", getSender(), channel, userToInvite);
    }
}

/**
 * Represents a {@link Command} issued by a client to remove another client from a channel owned by
 * the sender. Everyone in the initial channel (including the user being kicked) should be informed
 * that the user was kicked.
 */
class KickCommand extends Command {
    private String channel;
    private String userToKick;

    public KickCommand(int senderId, String sender, String channel, String userToKick) {
        super(senderId, sender);
        this.channel = channel;
        this.userToKick = userToKick;
    }

    @Override
    public Broadcast updateServerModel(ServerModel model) {
        // TODO: Handle kick command
    	Collection<String> previousUsers =  model.getUsersInChannel(this.getChannel());
    	
    	ServerError response = 
    			model.removeUserFromChannel(this.getUserToKick(), this.getChannel(), this.getSender());
    	
    	if (response == ServerError.OKAY) {
    		
    		return Broadcast.okay(this, previousUsers);
    	} else {
    		
    		return Broadcast.error(this, response);
    	}
    }
    
    public String getChannel() {
        return channel;
    }

    public String getUserToKick() {
        return userToKick;
    }
    @Override
    public String toString() {
        return String.format(":%s KICK %s %s", getSender(), channel, userToKick);
    }
}

