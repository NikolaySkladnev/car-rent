package carrental.repository;

import carrental.domain.model.Client;
import carrental.domain.model.ClientWithHash;

public interface ClientRepository {
    long create(Client client, String passwordHash);
    ClientWithHash getByLogin(String login);
    boolean existsEmail(String email);
}
