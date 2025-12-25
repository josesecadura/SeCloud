package modelos;

import java.io.Serializable;

public class User implements Serializable {
    private String name;
    private String email;
    private String password;
    private String phone;
    private String photo;

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public User(String name, String email, String password, String photo) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.photo = photo;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhotoUrl(String photo) {
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
