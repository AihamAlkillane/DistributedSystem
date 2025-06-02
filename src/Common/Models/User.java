package Common.Models;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private String role;
    private String token;

    public User(){}
    public User(String username, String password,String role)
    {
        this.role = role;
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String name) { this.username = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String geDepartment() {return role;}
    public void setRole(String role){this.role = role;}
    public String getToken() {return token;}
    public void setToken(String token){this.token = token;}
}
//    public static void main(String[] args) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        User user = new User("2aaa", "123");
//        try {
//            objectMapper.writeValue(new File("src/Services/users.json"), user);
//            System.out.println("JSON written to src.Models.User.json");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }