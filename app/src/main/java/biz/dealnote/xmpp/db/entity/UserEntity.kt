package biz.dealnote.xmpp.db.entity

class UserEntity(val id: Int,val jid: String) {
    var firstName: String? = null
    var lastName: String? = null
    var middleName: String? = null
    var prefix: String? = null
    var suffix: String? = null
    var emailHome: String? = null
    var emailWork: String? = null
    var organization: String? = null
    var organizationUnit: String? = null
    var photoMimeType: String? = null
    var photoHash: String? = null
    var lastVcardUpdateTime: Long? = null
}