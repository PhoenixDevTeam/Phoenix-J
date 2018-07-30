package biz.dealnote.xmpp.service.request.operation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;

import java.io.File;
import java.io.IOException;

import biz.dealnote.xmpp.Constants;
import biz.dealnote.xmpp.Extra;
import biz.dealnote.xmpp.Injection;
import biz.dealnote.xmpp.db.interfaces.IMessagesRepository;
import biz.dealnote.xmpp.exception.Codes;
import biz.dealnote.xmpp.exception.CustomAppException;
import biz.dealnote.xmpp.model.AppMessage;
import biz.dealnote.xmpp.model.MessageUpdate;
import biz.dealnote.xmpp.service.IXmppContext;
import biz.dealnote.xmpp.service.TransferNotFoundException;
import biz.dealnote.xmpp.service.request.Request;
import biz.dealnote.xmpp.service.request.exception.CustomRequestException;
import biz.dealnote.xmpp.transfer.IFileTransferer;
import biz.dealnote.xmpp.util.AppPerms;

public class IncomeFileAcceptOperation extends AbsXmppOperation {

    private static final String TAG = IncomeFileAcceptOperation.class.getSimpleName();

    private static File generateFile(String fileName) throws CustomAppException {
        File dir = new File(Constants.INCOME_FILES_DIRECTORY);
        if (!dir.isDirectory()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new CustomAppException("Unable to create directory " + dir.getAbsolutePath());
            }
        }

        File target = new File(dir, fileName);

        int count = 1;
        while (target.exists()) {
            String[] filenameArray = fileName.split("\\.");

            String newFileName;
            if (filenameArray.length == 1) {
                newFileName = fileName + "(" + count + ")";
            } else {
                String ext = filenameArray[filenameArray.length - 1];
                String fileNameWithoutExtension = fileName.substring(0, fileName.length() - ext.length() - 1);
                newFileName = fileNameWithoutExtension + "(" + count + ")." + ext;
            }

            target = new File(dir, newFileName);

            count++;
        }

        Log.d(TAG, "generateFile, target file: " + target);
        return target;
    }

    @Override
    public Bundle executeRequest(@NonNull Context context, @NonNull IXmppContext xmppContext, @NonNull Request request) throws CustomRequestException {
        int messageId = request.getInt(Extra.MESSAGE_ID);

        IFileTransferer transferer = Injection.INSTANCE.provideTransferer();
        IMessagesRepository repository = Injection.INSTANCE.provideRepositories().getMessages();

        FileTransferRequest transferRequest = transferer.findIncome(messageId);

        if (transferRequest == null) {
            repository.updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED))
                    .blockingAwait();

            throw new CustomAppException("The request does not exist", Codes.FILE_TRANSFER_REQUEST_IS_OUT_OF_DATE);
        }

        boolean hasPermission = AppPerms.hasWriteStoragePermision(context);
        if (!hasPermission) {
            throw new CustomAppException("No rights to write to the internal memory", Codes.NEED_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }

        File file = generateFile(transferRequest.getFileName());

        try {
            repository.updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_IN_PROGRESS))
                    .blockingAwait();

            transferer.acceptIncomeRequest(messageId, file);
        } catch (TransferNotFoundException | IOException | SmackException e) {
            repository.updateMessage(messageId, MessageUpdate.simpleStatusChange(AppMessage.STATUS_CANCELLED))
                    .blockingAwait();

            throw new CustomAppException(e.getMessage());
        }

        return null;
    }
}
