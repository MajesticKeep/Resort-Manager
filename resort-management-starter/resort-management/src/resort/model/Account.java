package resort.model;

import java.io.Serializable;

public abstract class Account implements Serializable {
    private String username;
    private String password;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }

    public abstract boolean isAdmin();
}
