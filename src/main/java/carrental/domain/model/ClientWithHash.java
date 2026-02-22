package carrental.domain.model;

public record ClientWithHash(
        Client client,
        String passwordHash
) {}
