/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sessionbeans;

import entityclass.AuditLogs;
import jakarta.ejb.Local;
import java.util.List;

/**
 *
 * @author TRUONG LAM
 */
@Local
public interface AuditLogsFacadeLocal {

    void create(AuditLogs auditLogs);

    void edit(AuditLogs auditLogs);

    void remove(AuditLogs auditLogs);

    AuditLogs find(Object id);

    List<AuditLogs> findAll();

    List<AuditLogs> findRange(int[] range);

    int count();
    
}
