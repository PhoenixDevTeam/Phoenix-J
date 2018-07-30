package biz.dealnote.xmpp.model;

/**
 * Created by ruslan.kolbasa on 02.11.2016.
 * phoenix_for_xmpp
 */
public class MessageBuilder {

    private int accountId;

    private String destination;
    private Integer interlocutorId;

    private String senderJid;
    private Integer senderId;

    private String body;
    private long date;
    private int type;
    private Integer chatId;
    private AppFile appFile;
    private boolean out;
    private int status;
    private String uniqueServiceId;
    private boolean readState;
    private boolean wasEncrypted;

    public MessageBuilder(int accountId) {
        this.accountId = accountId;
    }

    public MessageBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public MessageBuilder setSenderJid(String senderJid) {
        this.senderJid = senderJid;
        return this;
    }

    public MessageBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    public MessageBuilder setDate(long date) {
        this.date = date;
        return this;
    }

    public MessageBuilder setType(int type) {
        this.type = type;
        return this;
    }

    public MessageBuilder setChatId(Integer chatId) {
        this.chatId = chatId;
        return this;
    }

    public MessageBuilder setSenderId(Integer senderId) {
        this.senderId = senderId;
        return this;
    }

    public MessageBuilder setAppFile(AppFile appFile) {
        this.appFile = appFile;
        return this;
    }

    public MessageBuilder setOut(boolean out) {
        this.out = out;
        return this;
    }

    public MessageBuilder setStatus(int status) {
        this.status = status;
        return this;
    }

    public MessageBuilder setInterlocutorId(Integer interlocutorId) {
        this.interlocutorId = interlocutorId;
        return this;
    }

    public MessageBuilder setUniqueServiceId(String uniqueServiceId) {
        this.uniqueServiceId = uniqueServiceId;
        return this;
    }

    public MessageBuilder setReadState(boolean readState) {
        this.readState = readState;
        return this;
    }

    public boolean isWasEncrypted() {
        return wasEncrypted;
    }

    public MessageBuilder setWasEncrypted(boolean wasEncrypted) {
        this.wasEncrypted = wasEncrypted;
        return this;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getDestination() {
        return destination;
    }

    public Integer getInterlocutorId() {
        return interlocutorId;
    }

    public String getSenderJid() {
        return senderJid;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public String getBody() {
        return body;
    }

    public long getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    public Integer getChatId() {
        return chatId;
    }

    public AppFile getAppFile() {
        return appFile;
    }

    public boolean isOut() {
        return out;
    }

    public int getStatus() {
        return status;
    }


    public String getUniqueServiceId() {
        return uniqueServiceId;
    }

    public boolean isReadState() {
        return readState;
    }
}
