package biz.dealnote.xmpp.model;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by admin on 08.11.2016.
 * phoenix-for-xmpp
 */
public class MessageCriteria {

    private Integer accountId;

    private Integer chatId;

    private String destination;

    private Integer startMessageId;

    private Set<Integer> ignoreContactIds;

    private Set<Integer> forceLoadContactIds;

    private int count;

    public MessageCriteria() {
        count = 50;
    }

    public MessageCriteria setCount(int count) {
        this.count = count;
        return this;
    }

    public int getCount() {
        return count;
    }

    public MessageCriteria setAccountId(Integer accountId) {
        this.accountId = accountId;
        return this;
    }

    public Integer getAccountId() {
        return accountId;
    }

    @NonNull
    public Set<Integer> prepareIgnoreContactIds() {
        if (ignoreContactIds == null) {
            ignoreContactIds = new HashSet<>(2);
        }

        return ignoreContactIds;
    }

    public Set<Integer> getForceLoadContactIds() {
        return forceLoadContactIds;
    }

    @NonNull
    public Set<Integer> prepareForceLoadContactIds() {
        if (forceLoadContactIds == null) {
            forceLoadContactIds = new HashSet<>(1);
        }

        return forceLoadContactIds;
    }

    public Integer getStartMessageId() {
        return startMessageId;
    }

    public MessageCriteria setStartMessageId(Integer startMessageId) {
        this.startMessageId = startMessageId;
        return this;
    }

    public Integer getChatId() {
        return chatId;
    }

    public MessageCriteria setChatId(Integer chatId) {
        this.chatId = chatId;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public MessageCriteria setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public MessageCriteria setIgnoreContactIds(Set<Integer> ignoreContactIds) {
        this.ignoreContactIds = ignoreContactIds;
        return this;
    }

    public Set<Integer> getIgnoreContactIds() {
        return ignoreContactIds;
    }
}