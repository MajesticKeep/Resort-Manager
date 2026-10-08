package resort.model;

public class Employee extends Account {
    private static final long serialVersionUID = 8475601791189894288L;

    public Employee(String username, char[] password) {
        super(username, password, false);
    }

    Employee(String username) {
        super(username, false);
    }
}