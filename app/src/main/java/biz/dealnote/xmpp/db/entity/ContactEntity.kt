package biz.dealnote.xmpp.db.entity

class ContactEntity(val id: Int, val jid: String, val accountId: Int, val accountJid: String, val user: UserEntity) {
    var flags: Int = 0
    var availableToReceiveMessages: Boolean = false
    var away: Boolean = false
    var presenceMode: Int? = null
    var presenceType: Int? = null
    var presenceStatus: String? = null
    var type: Int? = null
    var nick: String? = null
    var priority: Int = 0
}