package biz.dealnote.xmpp.model;

public class InputMessageIntent {

    public int accountId;
    public String from;
    public String body;
    public long time;
    public String uniqueServiceId;
    public int appType;
    public boolean wasEncrypted;

    public InputMessageIntent setAccountId(int accountId) {
        this.accountId = accountId;
        return this;
    }

    public InputMessageIntent setFrom(String from) {
        this.from = from;
        return this;
    }

    public InputMessageIntent setBody(String body) {
        this.body = body;
        return this;
    }

    public InputMessageIntent setTime(long time) {
        this.time = time;
        return this;
    }

    public InputMessageIntent setUniqueServiceId(String uniqueServiceId) {
        this.uniqueServiceId = uniqueServiceId;
        return this;
    }

    public InputMessageIntent setAppType(int appType) {
        this.appType = appType;
        return this;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getFrom() {
        return from;
    }

    public String getBody() {
        return body;
    }

    public long getTime() {
        return time;
    }

    public String getUniqueServiceId() {
        return uniqueServiceId;
    }

    public int getAppType() {
        return appType;
    }

    public boolean isWasEncrypted() {
        return wasEncrypted;
    }

    public InputMessageIntent setWasEncrypted(boolean wasEncrypted) {
        this.wasEncrypted = wasEncrypted;
        return this;
    }
}
