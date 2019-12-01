import java.util.*;


/**
 * The {@code ServerModel} is the class responsible for tracking the
 * state of the server, including its current users and the channels
 * they are in.
 * This class is used by subclasses of {@link Command} to:
 *     1. handle commands from clients, and
 *     2. handle commands from {@link ServerBackend} to coordinate 
 *        client connection/disconnection. 
 */
public final class ServerModel implements ServerModelApi {
    
	private TreeMap<Integer, String> usersById;
	private TreeMap<String, Channel> channels;

	/**
     * Constructs a {@code ServerModel} and initializes any
     * collections needed for modeling the server state.
     */
    public ServerModel() {
        // TODO: Initialize your state here
    	this.usersById = new TreeMap<Integer, String>();
    	this.channels = new TreeMap<String, Channel>();
    }


    //==========================================================================
    // Client connection handlers
    //==========================================================================

    /**
     * Informs the model that a client has connected to the server
     * with the given user ID. The model should update its state so
     * that it can identify this user during later interactions. The
     * newly connected user will not yet have had the chance to set a
     * nickname, and so the model should provide a default nickname
     * for the user.  Any user who is registered with the server
     * (without being later deregistered) should appear in the output
     * of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID created by the backend to represent this user
     * @return A {@link Broadcast} to the user with their new nickname
     */
    public Broadcast registerUser(int userId) {
        // TODO: Return broadcast upon user connection
        String nickname = generateUniqueNickname();
        this.usersById.put(userId, nickname);
        
        return Broadcast.connected(nickname);
    }

    /**
     * Generates a unique nickname of the form "UserX", where X is the
     * smallest non-negative integer that yields a unique nickname for a user.
     * @return the generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers != null && existingUsers.contains(nickname));
        return nickname;
    }

    /**
     * Determines if a given nickname is valid or invalid (contains at least
     * one alphanumeric character, and no non-alphanumeric characters).
     * @param name The channel or nickname string to validate
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Informs the model that the client with the given user ID has
     * disconnected from the server.  After a user ID is deregistered,
     * the server backend is free to reassign this user ID to an
     * entirely different client; as such, the model should remove all
     * state of the user associated with the deregistered user ID. The
     * behavior of this method if the given user ID is not registered
     * with the model is undefined.  Any user who is deregistered
     * (without later being registered) should not appear in the
     * output of {@link #getRegisteredUsers()}.
     *
     * @param userId The unique ID of the user to deregister
     * @return A {@link Broadcast} instructing clients to remove the
     * user from all channels
     */
    public Broadcast deregisterUser(int userId) {
        // TODO: Return broadcast upon user deregistration
    	String nickname = this.usersById.get(userId);
    	LinkedList<String> recipients =  new LinkedList<String>();
    	recipients.remove(nickname);
    	
    	//remove the deregistered user from everyone's list
    	for (Channel channelName: this.channels.values()) {
    		if (channelName.getUsers().containsKey(userId)) {
    			if (channelName.getOwner().equals(nickname)) {
    				recipients.addAll(channelName.getUsers().values());
        			this.channels.remove(channelName.getName());
        		} else {
        		channelName.getUsers().remove(userId);	
        		recipients.addAll(channelName.getUsers().values());
        		}
    		}
    	}
        recipients.remove(nickname);
    	this.usersById.remove(userId);
    	
        return Broadcast.disconnected(nickname, recipients);
    }

    

    //==========================================================================
    // Model update functions
    //==========================================================================

    public ServerError createChannel(String channelName, String owner, boolean inviteOnly) {
        // TODO: Write me
    	TreeMap<Integer, String> singletonUser = new TreeMap<Integer, String>();
    	singletonUser.put(getUserId(owner), owner);
    	
    	Channel newChannel = new Channel(channelName, owner, inviteOnly, singletonUser);
    	
    	if (this.channels.containsKey(channelName)) {
    		
    		return ServerError.CHANNEL_ALREADY_EXISTS;
    	} else if (!isValidName(channelName)) {
    		
    		return ServerError.INVALID_NAME;
    	} else {
    		this.channels.put(channelName, newChannel);
    		
            return ServerError.OKAY;
    	}
    }

    public ServerError addUserToChannel(String userName, String channelName) {
        // TODO: Write me
    	int userId = getUserId(userName);
    	if (userId == -1) {
    		
    		return ServerError.NO_SUCH_USER;
    	} else if (!this.channels.containsKey(channelName)) {
    		
    		return ServerError.NO_SUCH_CHANNEL;
    	} else if (!isPublicChannel(channelName)) {
    		
    		return ServerError.JOIN_PRIVATE_CHANNEL;
    	}
    	else {
    		this.channels.get(channelName).addUser(userId, userName);
    		
    		return ServerError.OKAY;
    	}
    }
    
    public ServerError addUserToPrivateChannel(String userName, String channelName, String sender) {
    	int userId = getUserId(userName);
    	
    	if (userId == -1) {
    		
    		return ServerError.NO_SUCH_USER;
    	} else if (!this.channels.containsKey(channelName)){
    		
    		return ServerError.NO_SUCH_CHANNEL;
    	} else if (isPublicChannel(channelName)) {
    		
    		return ServerError.INVITE_TO_PUBLIC_CHANNEL;
    	} else if (!this.channels.get(channelName).getOwner().equals(sender)) {
    		
    		return ServerError.USER_NOT_OWNER;
    	} else {
    		this.channels.get(channelName).addUser(userId, userName);
    		
    		return ServerError.OKAY;
    	}
    }
    
    public ServerError removeUserFromChannel(String username, String channelName, String sender) {
    	int id = getUserId(username);
    	
    	if (id == -1) {
    		
    		return ServerError.NO_SUCH_USER;
    	} else if (!this.channels.containsKey(channelName)) {
    		
    		return ServerError.NO_SUCH_CHANNEL;
    	} else if (!this.channels.get(channelName).getOwner().equals(sender)) {
    		
    		return ServerError.USER_NOT_OWNER;
    	} else if (!this.channels.get(channelName).getUsers().containsKey(id)) {
    		
    		return ServerError.USER_NOT_IN_CHANNEL;
    	} else if (this.channels.get(channelName).getOwner().equals(username)) {
    		this.channels.remove(channelName);
			
			return ServerError.OKAY;
    	}
    	else {
    		this.channels.get(channelName).removeUser(id);
    		
    		return ServerError.OKAY;
    	} 	
    }

    public Broadcast changeNickname(NicknameCommand command) {
        // TODO: Write me
    	LinkedList<String> recipients = new LinkedList<String>();
    	int id = command.getSenderId();
    	String newNickname = command.getNewNickname();
    	
    	if (this.usersById.values().contains(newNickname)) {
    		
    		return Broadcast.error(command, ServerError.NAME_ALREADY_IN_USE);
    	} else if (!isValidName(newNickname)) {
    		
    		return Broadcast.error(command, ServerError.INVALID_NAME);
    	}
    	
       this.usersById.put(id, newNickname);
       
       for (Channel channel: this.channels.values()) {
    	   if (channel.getUsers().containsKey(id)) {
        	   channel.getUsers().put(id, newNickname);
        	   recipients.addAll(channel.getUsers().values());
    	   }
       }
       
       return Broadcast.okay(command, recipients);
    }

    public boolean isPublicChannel(String channelName) {
        // TODO: Write me		
    	Boolean isPublic = !channels.get(channelName).getInvite();
    	
    	return isPublic;
    }

    //==========================================================================
    // Server model queries
    // These functions provide helpful ways to test the state of your model.
    // You may also use them in your implementation.
    //==========================================================================

    /**
     * Gets the user ID currently associated with the given
     * nickname. The returned ID is -1 if the nickname is not
     * currently in use.
     *
     * @param nickname The nickname for which to get the associated user ID
     * @return The user ID of the user with the argued nickname if
     * such a user exists, otherwise -1
     */
    public int getUserId(String nickname) {
        // TODO: Return user ID corresponding to nickname
    	for (int id: this.usersById.keySet()) {
    		if (this.usersById.get(id).contentEquals(nickname)) {
    			int clonedId = id;
    			
    			return clonedId;
    		}
    	}
        return -1;
    }

    /**
     * Gets the nickname currently associated with the given user
     * ID. The returned nickname is null if the user ID is not
     * currently in use.
     *
     * @param userId The user ID for which to get the associated
     *        nickname
     * @return The nickname of the user with the argued user ID if
     *          such a user exists, otherwise null
     */
    public String getNickname(int userId) {
        // TODO: Return nickname corresponding to ID
    	String nickname = usersById.get(userId);
    	
        return nickname;
    }

    /**
     * Gets a collection of the nicknames of all users who are
     * registered with the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
        // TODO: Return users connected to server
    	Collection<String> clonedUsers = new TreeSet<String>(); 
    	clonedUsers.addAll(usersById.values());
    	
        return clonedUsers;
    }

    /**
     * Gets a collection of the names of all the channels that are
     * present on the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of channel names
     */
    public Collection<String> getChannels() {
        // TODO: Return channels on server
    	Collection<String> clonedChannels = new TreeSet<String>();
    	clonedChannels.addAll(channels.keySet());
    	
        return clonedChannels;
    }

    /**
     * Gets a collection of the nicknames of all the users in a given
     * channel. The collection is empty if no channel with the given
     * name exists. Modifications to the returned collection should
     * not affect the server state.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get member nicknames
     * @return The collection of user nicknames in the argued channel
     */
    public Collection<String> getUsersInChannel(String channelName) {
        // TODO: Return users in the channel
    	if (this.channels.containsKey(channelName)) {
    		Collection<String> clonedUsers = new TreeSet<String>();
    		clonedUsers.addAll(channels.get(channelName).getUsers().values());
    			
    		return clonedUsers;
    	}
    	
    	return Collections.emptySet();
    }

    /**
     * Gets the nickname of the owner of the given channel. The result
     * is {@code null} if no channel with the given name exists.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get the owner nickname
     * @return The nickname of the channel owner if such a channel
     * exists, othewrise null
     */
    public String getOwner(String channelName) {
        // TODO: Return owner of the channel
    	if (this.channels.containsKey(channelName)) {
    		String owner = channels.get(channelName).getOwner();
    		
    		return owner;
    	}
    	
        return null;
    }
}
