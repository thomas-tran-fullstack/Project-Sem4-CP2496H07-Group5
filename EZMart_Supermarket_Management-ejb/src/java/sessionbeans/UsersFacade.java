/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sessionbeans;

import entityclass.Users;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;

/**
 *
 * @author TRUONG LAM
 */
@Stateless
public class UsersFacade extends AbstractFacade<Users> implements UsersFacadeLocal {

    @PersistenceContext(unitName = "EZMart_Supermarket-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UsersFacade() {
        super(Users.class);
    }
    
     public Users findByUsernameAndPassword(String username, String passwordHash) {
        try {
            return em.createQuery("SELECT u FROM Users u WHERE u.username = :username AND u.passwordHash = :passwordHash", Users.class)
                    .setParameter("username", username)
                    .setParameter("passwordHash", passwordHash)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    
    public Users findByUsername(String username) {
        try {
            return em.createNamedQuery("Users.findByUsername", Users.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public Users findByEmail(String email) {
        try {
            return em.createNamedQuery("Users.findByEmail", Users.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public Users findByIdentifierAndPassword(String identifier, String passwordHash) {
        try {
            Users u = null;
            // try username
            try {
                u = em.createNamedQuery("Users.findByUsername", Users.class)
                        .setParameter("username", identifier)
                        .getSingleResult();
            } catch (NoResultException nre) {
                // try email
                try {
                    u = em.createNamedQuery("Users.findByEmail", Users.class)
                            .setParameter("email", identifier)
                            .getSingleResult();
                } catch (NoResultException ex) {
                    return null;
                }
            }
            if (u != null && passwordHash != null && passwordHash.equals(u.getPasswordHash())) {
                return u;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public java.util.List<Users> findUsersByRole(String role) {
        try {
            return em.createQuery("SELECT u FROM Users u WHERE u.role = :role", Users.class)
                    .setParameter("role", role)
                    .getResultList();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
    
}
