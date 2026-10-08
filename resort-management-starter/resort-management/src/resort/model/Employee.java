package resort.model;

public class Employee extends Account {

    public Employee(String username, String password) {
        super(username, password);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}