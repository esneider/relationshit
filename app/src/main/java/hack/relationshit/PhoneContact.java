package hack.relationshit;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import hack.relationshit.http.Message;

public class PhoneContact {

    private static HashMap<String,PhoneContact> NUMBERS_TO_CONTACTS = null;
    private static HashMap<String,PhoneContact> NAMES_TO_CONTACTS = null;

    private String number;
    private String name;
    private String imageUri;
    private int score;
    private int sentTo = 0;
    private int receivedFrom = 0;

    public static PhoneContact byNumber(Context context, String number) {
        populateContactNumbers(context);

        if (NUMBERS_TO_CONTACTS.get(number) != null) {
            return NUMBERS_TO_CONTACTS.get(number);
        } else {
            PhoneContact phoneContact = blankUser();
            phoneContact.number = number;
            phoneContact.name = number;
            NUMBERS_TO_CONTACTS.put(number, phoneContact);
            NAMES_TO_CONTACTS.put(number, phoneContact);
            return phoneContact;
        }
    }

    public static PhoneContact byName(Context context, String name) {
        populateContactNumbers(context);

        if (NAMES_TO_CONTACTS.get(name) != null) {
            return NAMES_TO_CONTACTS.get(name);
        } else {
            PhoneContact phoneContact = blankUser();
            phoneContact.number = name;
            phoneContact.name = name;
            return phoneContact;
        }
    }

    private static PhoneContact blankUser() {
        PhoneContact phoneContact = new PhoneContact();
        phoneContact.score = 0;
        phoneContact.sentTo = 0;
        phoneContact.receivedFrom = 0;
        return phoneContact;
    }

    public static List<PhoneContact> allContacts(Context context) {
        populateContactNumbers(context);

        return new ArrayList<>(NUMBERS_TO_CONTACTS.values());
    }

    public static void reset() {
        NUMBERS_TO_CONTACTS = null;
        NAMES_TO_CONTACTS = null;
    }

    private static void populateContactNumbers(Context context) {
        if(NUMBERS_TO_CONTACTS == null) {
            NUMBERS_TO_CONTACTS = new HashMap<>();
            NAMES_TO_CONTACTS = new HashMap<>();
            ContentResolver cr = context.getContentResolver(); //Activity/Application android.content.Context
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String image_uri = cursor.getString(cursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    String number = getContactNumber(cr, cursor);

                    PhoneContact phoneContact = blankUser();
                    phoneContact.imageUri = image_uri;
                    phoneContact.name = name;
                    phoneContact.number = number;
                    phoneContact.receivedFrom = 0;
                    phoneContact.sentTo = 0;

                    NAMES_TO_CONTACTS.put(name, phoneContact);
                    if (phoneContact.getNumber() != null) {
                        NUMBERS_TO_CONTACTS.put(phoneContact.getNumber(), phoneContact);
                    }

                } while (cursor.moveToNext());
            }
            cursor.close();

            populateMessageNumbers(context);
        }
    }

    private static void populateMessageNumbers(Context context) {
        for (Message message : SMSes.getMessages(context)) {
            PhoneContact phoneContact = byNumber(context, message.getPhoneNumber());
            phoneContact.score++;
            if(message.getDirection().equals("send")) {
                phoneContact.sentTo++;
            } else if(message.getDirection().equals("receive")) {
                phoneContact.receivedFrom++;
            }
        }
    }

    private static String getContactNumber(ContentResolver cr, Cursor cursor) {
        if(Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0)
        {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",new String[]{ id }, null);
            while (pCur.moveToNext())
            {
                String number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                pCur.close();
                return CustomPhoneNumberUtils.normalizeNumber(number);
            }
        }

        return null;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int score() {
        return score;
    }

    public int getSentTo() {
        return sentTo;
    }

    public int getReceivedFrom() {
        return receivedFrom;
    }

    public Bitmap getImage(Context context) {
        if(imageUri != null) {
            try {
                return MediaStore.Images.Media
                        .getBitmap(context.getContentResolver(),
                                Uri.parse(imageUri));
            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.unknown);
    }
}
