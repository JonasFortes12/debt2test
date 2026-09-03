package fixture;

public class ProjectFixture {

    // TODO: simplify this method.
    public void documented() {
        // TODO: this inline comment is not attached to a method.
    }

    public void withoutComment() {
    }

    static class Nested {

        /* TODO: handle the integer overload. */ void overloaded(int value) {} /* TODO: handle the string overload. */ void overloaded(String value) {}
    }
}
