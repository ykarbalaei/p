package io.github.some_example_name.model;

public record Result(boolean success, String message) {

    public static Result success(String message) {
        return new Result(true, message);
    }

    public static Result failure(String message) {
        return new Result(false, message);
    }
}
