package sessionbeans;

import entityclass.ChatMessages;
import jakarta.ejb.Local;
import java.util.List;

/**
 * Local interface for ChatMessagesFacade
 */
@Local
public interface ChatMessagesFacadeLocal {

    void create(ChatMessages chatMessages);

    void edit(ChatMessages chatMessages);

    void remove(ChatMessages chatMessages);

    ChatMessages find(Object id);

    List<ChatMessages> findAll();

    List<ChatMessages> findRange(int[] range);

    int count();

    List<ChatMessages> findByConversationID(Integer conversationID);

    List<ChatMessages> findByConversationIDOrdered(Integer conversationID);

    List<ChatMessages> findRecentByConversationID(Integer conversationID, int limit);

    int countUnreadByConversation(Integer conversationID, String excludeRole, Integer excludeUserID);

    void markAsRead(Integer conversationID, String currentRole, Integer currentUserID);
}
