package kz.kimep.ticketing.messaging.service;

public interface Notifier {

    void send(String email, String subject, String body);
}
