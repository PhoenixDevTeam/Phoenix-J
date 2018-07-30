package biz.dealnote.xmpp.model

data class ChatUpdateModel(val chatId: Int,
                           val unreadCountUpdate: UnreadCountUpdate? = null,
                           val lastMessageUpdate: LastMessageUpdate? = null,
                           val hiddenUpdate: HiddenUpdate? = null)

data class UnreadCountUpdate(val count: Int)

data class HiddenUpdate(val isHidden: Boolean)

data class LastMessageUpdate(val lastMessageText: String? = null,
                             val lastMessageTime: Long,
                             val lastMessageOut: Boolean,
                             val lastMessageType: Int)