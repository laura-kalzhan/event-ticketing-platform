package kz.kimep.ticketing.identity.service;

public record AuthenticatedUser(Long id, String email, String role) {

    public static final String ADMIN = "ADMIN";
    public static final String ORGANIZER = "ORGANIZER";

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }

    public boolean isOrganizer() {
        return ORGANIZER.equals(role) || isAdmin();
    }
}
