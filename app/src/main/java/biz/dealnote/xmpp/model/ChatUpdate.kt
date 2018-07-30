package biz.dealnote.xmpp.model

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
class ChatUpdate(val chatId: Int) {

    private var unreadCountUpdate: UnreadCountUpdate? = null

    private var lastMessageUpdate: LastMessageUpdate? = null

    private var hiddenUpdate: HiddenUpdate? = null

    fun getUnreadCountUpdate(): UnreadCountUpdate? {
        return unreadCountUpdate
    }

    fun setUnreadCountUpdate(unreadCountUpdate: UnreadCountUpdate): ChatUpdate {
        this.unreadCountUpdate = unreadCountUpdate
        return this
    }

    fun setLastMessageUpdate(lastMessageUpdate: LastMessageUpdate): ChatUpdate {
        this.lastMessageUpdate = lastMessageUpdate
        return this
    }

    fun getLastMessageUpdate(): LastMessageUpdate? {
        return lastMessageUpdate
    }

    fun getHiddenUpdate(): HiddenUpdate? {
        return hiddenUpdate
    }

    fun setHiddenUpdate(hiddenUpdate: HiddenUpdate): ChatUpdate {
        this.hiddenUpdate = hiddenUpdate
        return this
    }

    class UnreadCountUpdate(val count: Int)

    class HiddenUpdate(val isHidden: Boolean)

    class LastMessageUpdate {

        private var lastMessageText: String? = null

        private var lastMessageTime: Long = 0

        private var lastMessageOut: Boolean = false

        private var lastMessageType: Int = 0

        fun getLastMessageText(): String? {
            return lastMessageText
        }

        fun setLastMessageText(lastMessageText: String): LastMessageUpdate {
            this.lastMessageText = lastMessageText
            return this
        }

        fun getLastMessageTime(): Long {
            return lastMessageTime
        }

        fun setLastMessageTime(lastMessageTime: Long): LastMessageUpdate {
            this.lastMessageTime = lastMessageTime
            return this
        }

        fun isLastMessageOut(): Boolean {
            return lastMessageOut
        }

        fun setLastMessageOut(lastMessageOut: Boolean): LastMessageUpdate {
            this.lastMessageOut = lastMessageOut
            return this
        }

        fun getLastMessageType(): Int {
            return lastMessageType
        }

        fun setLastMessageType(lastMessageType: Int): LastMessageUpdate {
            this.lastMessageType = lastMessageType
            return this
        }
    }
}