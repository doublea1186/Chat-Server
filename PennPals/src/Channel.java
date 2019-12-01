import java.util.TreeMap;

public class Channel {
	
	private String name;
	private String owner;
	private TreeMap<Integer, String> users; 
	private Boolean inviteOnly;
	
	public Channel(String name, String owner, Boolean inviteOnly, TreeMap<Integer, String> users) {
		this.name = name;
		this.owner = owner;
		this.users = users;
		this.inviteOnly = inviteOnly;
	}
	
	public Boolean getInvite() {
		return inviteOnly;
	}
	
	public void setInvite(Boolean inviteOnly) {
		this.inviteOnly = inviteOnly;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public TreeMap<Integer, String> getUsers() {
		return users;
	}

	public void setUsers(TreeMap<Integer, String> users) {
		this.users = users;
	}
	
	public void addUser(int id, String nickname) {
		users.put(id, nickname);
	}
	
	public void removeUser(int id) {
		users.remove(id);
	}
}
