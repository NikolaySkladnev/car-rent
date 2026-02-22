package carrental.api.dto;

public record RegisterRequest(
        String fullName,
        String passportData,
        String login,
        String password,
        String email,
        String phone,
        String address
) {}
