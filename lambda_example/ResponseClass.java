package com.smartfarm.www;

// AWS 람다 함수는 입력값과 출력값이 JSON 구조이기 때문에
// JSON을 자바로 구현하려면 getter setter로 구현하여 주어야만 한다.

public class ResponseClass {
    String greetings;
    String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getGreetings() {
        return greetings;
    }

    public void setGreetings(String greetings) {
        this.greetings = greetings;
    }

    public ResponseClass(String greetings) {
        this.greetings = greetings;
    }

    public ResponseClass() {
    }
}