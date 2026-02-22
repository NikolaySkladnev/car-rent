package carrental.domain.model;

public record Client(
        long clientId,
        String fullName,
        String passportData,
        String login,
        String email,
        String phone,
        String address
) {}
