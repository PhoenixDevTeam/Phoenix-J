package biz.dealnote.xmpp.util

interface IStanzaIdGenerator {
    fun next(): String
}