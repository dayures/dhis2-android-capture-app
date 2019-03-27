package org.dhis2.data.qr;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.usescases.qrCodes.QrViewModel;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import timber.log.Timber;

import static org.dhis2.data.qr.QRjson.ATTR_JSON;
import static org.dhis2.data.qr.QRjson.DATA_JSON;
import static org.dhis2.data.qr.QRjson.DATA_JSON_WO_REGISTRATION;
import static org.dhis2.data.qr.QRjson.ENROLLMENT_JSON;
import static org.dhis2.data.qr.QRjson.EVENTS_JSON;
import static org.dhis2.data.qr.QRjson.EVENT_JSON;
import static org.dhis2.data.qr.QRjson.TEI_JSON;
import static org.dhis2.utils.SqlConstants.SELECT_ALL_FROM;
import static org.dhis2.utils.SqlConstants.WHERE;

/**
 * QUADRAM. Created by ppajuelo on 22/05/2018.
 */

public class QRCodeGenerator implements QRInterface {

    private final BriteDatabase briteDatabase;
    private final Gson gson;

    private static final String TEI = SELECT_ALL_FROM + SqlConstants.TEI_TABLE + WHERE + SqlConstants.TEI_TABLE + "." + SqlConstants.TEI_UID + " = ? LIMIT 1";

    private static final String EVENT = SELECT_ALL_FROM + SqlConstants.EVENT_TABLE + WHERE + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_UID + " = ? LIMIT 1";

    private static final String TEI_ATTR = SELECT_ALL_FROM + SqlConstants.TE_ATTR_VALUE_TABLE + WHERE + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TEI + " = ?";

    private static final String TEI_DATA = SELECT_ALL_FROM + SqlConstants.TEI_DATA_VALUE_TABLE + WHERE + SqlConstants.TEI_DATA_VALUE_TABLE + "." + SqlConstants.TEI_DATA_VALUE_EVENT + " = ?";

    private static final String TEI_ENROLLMENTS = SELECT_ALL_FROM + SqlConstants.ENROLLMENT_TABLE + WHERE + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + " = ?";

    private static final String TEI_EVENTS = SELECT_ALL_FROM + SqlConstants.EVENT_TABLE + WHERE + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_ENROLLMENT + " =?";

    QRCodeGenerator(BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
        gson = new GsonBuilder().setDateFormat(DateUtils.DATABASE_FORMAT_EXPRESSION).create();
    }

    @Override
    public Observable<List<QrViewModel>> teiQRs(String teiUid) {
        List<QrViewModel> bitmaps = new ArrayList<>();

        return
                briteDatabase.createQuery(SqlConstants.TEI_TABLE, TEI, teiUid == null ? "" : teiUid)
                        .mapToOne(TrackedEntityInstance::create)
                        .map(data -> bitmaps.add(new QrViewModel(TEI_JSON, gson.toJson(data))))


                        .flatMap(data -> briteDatabase.createQuery(SqlConstants.TE_ATTR_VALUE_TABLE, TEI_ATTR, teiUid == null ? "" : teiUid)
                                .mapToList(TrackedEntityAttributeValue::create))
                        .map(data -> {
                            ArrayList<TrackedEntityAttributeValue> arrayListAux = new ArrayList<>();
                            // DIVIDE ATTR QR GENERATION -> 1 QR PER 2 ATTR
                            int count = 0;
                            for (int i = 0; i < data.size(); i++) {
                                arrayListAux.add(data.get(i));
                                if (count == 1) {
                                    count = 0;
                                    bitmaps.add(new QrViewModel(ATTR_JSON, gson.toJson(arrayListAux)));
                                    arrayListAux.clear();
                                } else if (i == data.size() - 1) {
                                    bitmaps.add(new QrViewModel(ATTR_JSON, gson.toJson(arrayListAux)));
                                } else {
                                    count++;
                                }
                            }
                            return true;
                        })


                        .flatMap(data -> briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, TEI_ENROLLMENTS, teiUid == null ? "" : teiUid)
                                .mapToList(Enrollment::create))
                        .map(data -> {
                            ArrayList<Enrollment> arrayListAux = new ArrayList<>();
                            // DIVIDE ENROLLMENT QR GENERATION -> 1 QR PER 2 ENROLLMENT
                            int count = 0;
                            for (int i = 0; i < data.size(); i++) {
                                arrayListAux.add(data.get(i));
                                if (count == 1) {
                                    count = 0;
                                    bitmaps.add(new QrViewModel(ENROLLMENT_JSON, gson.toJson(arrayListAux)));
                                    arrayListAux.clear();
                                } else if (i == data.size() - 1) {
                                    bitmaps.add(new QrViewModel(ENROLLMENT_JSON, gson.toJson(arrayListAux)));
                                } else {
                                    count++;
                                }
                            }
                            return data;
                        })


                        .flatMap(data ->
                                Observable.fromIterable(data)
                                        .flatMap(enrollment -> briteDatabase.createQuery(SqlConstants.EVENT_TABLE, TEI_EVENTS, enrollment.uid() == null ? "" : enrollment.uid())
                                                .mapToList(Event::create)
                                        )
                        )
                        .flatMap(data ->
                                Observable.fromIterable(data)
                                        .flatMap(event -> {
                                                    bitmaps.add(new QrViewModel(EVENTS_JSON, gson.toJson(event)));
                                                    return briteDatabase.createQuery(SqlConstants.TEI_DATA_VALUE_TABLE, TEI_DATA, event.uid())
                                                            .mapToList(TrackedEntityDataValue::create)
                                                            .map(dataValueList -> {
                                                                ArrayList<TrackedEntityDataValue> arrayListAux = new ArrayList<>();
                                                                // DIVIDE ATTR QR GENERATION -> 1 QR PER 2 ATTR
                                                                int count = 0;
                                                                for (int i = 0; i < dataValueList.size(); i++) {
                                                                    arrayListAux.add(dataValueList.get(i));
                                                                    if (count == 1) {
                                                                        count = 0;
                                                                        bitmaps.add(new QrViewModel(DATA_JSON, gson.toJson(arrayListAux)));
                                                                        arrayListAux.clear();
                                                                    } else if (i == dataValueList.size() - 1) {
                                                                        bitmaps.add(new QrViewModel(DATA_JSON, gson.toJson(arrayListAux)));
                                                                    } else {
                                                                        count++;
                                                                    }
                                                                }
                                                                return true;
                                                            });
                                                }
                                        )
                        )
                        .map(data -> bitmaps);
    }


    @Override
    public Observable<List<QrViewModel>> eventWORegistrationQRs(String eventUid) {
        List<QrViewModel> bitmaps = new ArrayList<>();

        return
                briteDatabase.createQuery(SqlConstants.EVENT_TABLE, EVENT, eventUid == null ? "" : eventUid)
                        .mapToOne(Event::create)
                        .map(data -> {
                            bitmaps.add(new QrViewModel(EVENT_JSON, gson.toJson(data)));
                            return data;
                        })
                        .flatMap(data -> briteDatabase.createQuery(SqlConstants.TEI_DATA_VALUE_TABLE, TEI_DATA, data.uid() == null ? "" : data.uid())
                                .mapToList(TrackedEntityDataValue::create))
                        .map(data -> {
                            ArrayList<TrackedEntityDataValue> arrayListAux = new ArrayList<>();
                            // DIVIDE ATTR QR GENERATION -> 1 QR PER 2 ATTR
                            int count = 0;
                            for (int i = 0; i < data.size(); i++) {
                                arrayListAux.add(data.get(i));
                                if (count == 1) {
                                    count = 0;
                                    bitmaps.add(new QrViewModel(DATA_JSON_WO_REGISTRATION, gson.toJson(arrayListAux)));
                                    arrayListAux.clear();
                                } else if (i == data.size() - 1) {
                                    bitmaps.add(new QrViewModel(DATA_JSON_WO_REGISTRATION, gson.toJson(arrayListAux)));
                                } else {
                                    count++;
                                }
                            }
                            return true;
                        })
                        .map(data -> bitmaps);
    }

    public static Bitmap transform(String type, String info) {
        byte[] data;
        String encoded;
        data = info.getBytes(StandardCharsets.UTF_8);
        encoded = Base64.encodeToString(data, Base64.DEFAULT);

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        Bitmap bitmap = null;
        Gson gson = new GsonBuilder().setDateFormat(DateUtils.DATABASE_FORMAT_EXPRESSION).create();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(gson.toJson(new QRjson(type, encoded)), BarcodeFormat.QR_CODE, 1000, 1000);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            Timber.e(e);
        }

        return bitmap;
    }
}
