package sessionbeans;

import entityclass.PersistentLogins;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface PersistentLoginsFacadeLocal {

    void create(PersistentLogins persistentLogins);

    void edit(PersistentLogins persistentLogins);

    void remove(PersistentLogins persistentLogins);

    PersistentLogins find(Object id);

    List<PersistentLogins> findAll();

    List<PersistentLogins> findRange(int[] range);

    int count();

    /**
     * Find token by selector
     *
     * @param selector The unique selector
     * @return PersistentLogins record or null
     */
    PersistentLogins findBySelector(String selector);

    /**
     * Find all tokens for a user
     *
     * @param userID The user ID
     * @return List of PersistentLogins records
     */
    List<PersistentLogins> findByUser(Integer userID);

    /**
     * Delete expired tokens
     */
    void deleteExpired();
}
