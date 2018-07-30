package biz.dealnote.xmpp.util;

import android.os.Parcel;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ParcelUtils {

    public static void writeKeyPair(Parcel dest, KeyPair value){
        boolean valid = value != null && value.getPrivate() != null && value.getPublic() != null;
        dest.writeByte(valid ? (byte) 1 : (byte) 0);
        if(valid){
            dest.writeString(value.getPublic().getAlgorithm());
            dest.writeByteArray(value.getPublic().getEncoded());

            dest.writeString(value.getPrivate().getAlgorithm());
            dest.writeByteArray(value.getPrivate().getEncoded());
        }
    }

    public static KeyPair readKeyPair(Parcel in){
        boolean valid = in.readByte() == (byte) 1;
        if(valid){
            String pubAlgoritm = in.readString();
            byte[] pubBytes = in.createByteArray();

            String privAlgoritm = in.readString();
            byte[] privBytes = in.createByteArray();

            try {
                KeyFactory pubKeyFactory = KeyFactory.getInstance(pubAlgoritm);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubBytes);
                PublicKey publicKey = pubKeyFactory.generatePublic(publicKeySpec);

                KeyFactory privKeyFactory = KeyFactory.getInstance(privAlgoritm);
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privBytes);
                PrivateKey privateKey = privKeyFactory.generatePrivate(privateKeySpec);

                return new KeyPair(publicKey, privateKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                return null;
            }
        }

        return null;
    }

    public static void writeObjectDouble(Parcel dest, Double value) {
        dest.writeByte(value == null ? (byte) 1 : (byte) 0);
        if (value != null) {
            dest.writeDouble(value);
        }
    }

    public static Double readObjectDouble(Parcel in) {
        boolean isNull = in.readByte() == (byte) 1;
        if (!isNull) {
            return in.readDouble();
        } else return null;
    }

    public static void writeObjectBoolean(Parcel dest, Boolean value) {
        dest.writeByte(value == null ? (byte) 1 : (byte) 0);
        if (value != null) {
            dest.writeByte(value ? (byte) 1 : (byte) 0);
        }
    }

    public static Boolean readObjectBoolean(Parcel in) {
        boolean isNull = in.readByte() == (byte) 1;
        if (!isNull) {
            return in.readByte() == (byte) 1;
        } else return null;
    }

    public static void writeObjectInteger(Parcel dest, Integer value) {
        dest.writeByte(value == null ? (byte) 1 : (byte) 0);
        if (value != null) {
            dest.writeInt(value);
        }
    }

    public static Integer readObjectInteger(Parcel in) {
        boolean isNull = in.readByte() == (byte) 1;
        if (!isNull) {
            return in.readInt();
        } else return null;
    }

    public static void writeObjectLong(Parcel dest, Long value) {
        dest.writeByte(value == null ? (byte) 1 : (byte) 0);
        if (value != null) {
            dest.writeLong(value);
        }
    }

    public static Long readObjectLong(Parcel in) {
        boolean isNull = in.readByte() == (byte) 1;
        if (!isNull) {
            return in.readLong();
        } else return null;
    }

}
