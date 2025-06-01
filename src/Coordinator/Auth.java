package Coordinator;

import Common.Models.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.Map;
import java.util.UUID;

public class Auth {
    private List<User> users;
    public Map<String ,User> onlineUsers = new HashMap<>(); // key token , value User
    public Auth() {
        loadUsers();
    }
    public void loadUsers() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            users = mapper.readValue(new File("C:\\Users\\USER\\IdeaProjects\\DistributedFileSys\\src\\Coordinator\\users.json"), new TypeReference<List<User>>() {
            });
        } catch (IOException e) {
            System.out.println("could not load users");
        }
    }
    public User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                String token = UUID.randomUUID().toString();
                user.setToken(token);
                onlineUsers.put(token,user);
                return user;
            }
        }
        return null;
    }
    public boolean logout(String token) {
        if(onlineUsers.remove(token) == null)
            return false;
        return true;
    }
    public boolean authenticate(String token){
        return onlineUsers.containsKey(token);
    }
    public String getDepartment(String token){
     return onlineUsers.get(token).getRole();
    }
}
