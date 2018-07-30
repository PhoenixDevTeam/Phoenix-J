package biz.dealnote.xmpp.service

import biz.dealnote.xmpp.model.Account

data class AccountAction<T>(val account: Account, val data: T)