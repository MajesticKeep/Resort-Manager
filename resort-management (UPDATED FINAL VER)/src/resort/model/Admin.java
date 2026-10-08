package resort.model;

public class Admin extends Account {
    private static final long serialVersionUID = 5390203282765632553L;

    public Admin(String username, char[] password) {
        super(username, password, true);
    }

    Admin(String username) {
        super(username, true);
    }
}