package fixture;

public class AnonymousSameLineTypes {
    void create() { Object first = new Object() { /* TODO: first anonymous method. */ void same() {} }; Object second = new Object() { /* TODO: second anonymous method. */ void same() {} }; }
}
