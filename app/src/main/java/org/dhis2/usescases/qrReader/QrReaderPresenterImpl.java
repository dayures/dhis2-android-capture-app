package org.dhis2.usescases.qrReader;

import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.common.Coordinates;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.dhis2.utils.DateUtils.DATABASE_FORMAT_EXPRESSION;
import static org.dhis2.utils.SqlConstants.FROM;
import static org.dhis2.utils.SqlConstants.SELECT;
import static org.dhis2.utils.SqlConstants.SELECT_ALL_FROM;
import static org.dhis2.utils.SqlConstants.WHERE;

/**
 * QUADRAM. Created by ppajuelo on 22/05/2018.
 */

class QrReaderPresenterImpl implements QrReaderContracts.Presenter {

    private static final String DATA_ELEMENT = "dataElement";
    private static final String STORED_BY = "storedBy";
    private static final String VALUE = "value";
    private static final String PROVIDED_ELSEWHERE = "providedElsewhere";
    private static final String CREATED = "created";
    private static final String LAST_UPDATED = "lastUpdated";
    private static final String EVENT = "event";
    private static final String TE_ATTR = "trackedEntityAttribute";
    private static final String PROGRAM = "program";
    private static final String ENROLLMENT = "enrollment";
    private static final String STATE = "state";
    private static final String ORG_UNIT = "organisationUnit";
    private static final String TE_INSTANCE = "trackedEntityInstance";
    private static final String PROGRAM_STAGE = "programStage";
    private static final String EVENT_DATE = "eventDate";
    private static final String STATUS = "status";
    private static final String ATTR_OPTION_COMBO = "attributeOptionCombo";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String COMPLETED_DATE = "completedDate";
    private static final String DUE_DATE = "dueDate";
    private static final String LOG_INSERT = "insert event %l";

    private final BriteDatabase briteDatabase;
    private final D2 d2;
    private QrReaderContracts.View view;
    private CompositeDisposable compositeDisposable;

    private JSONObject eventWORegistrationJson;
    private String eventUid;
    private ArrayList<JSONObject> dataJson = new ArrayList<>();
    private ArrayList<JSONObject> teiDataJson = new ArrayList<>();

    private JSONObject teiJson;
    private List<JSONArray> attrJson = new ArrayList<>();
    private List<JSONArray> enrollmentJson = new ArrayList<>();
    private List<JSONArray> relationshipsJson = new ArrayList<>();
    private ArrayList<JSONObject> eventsJson = new ArrayList<>();
    private String teiUid;

    QrReaderPresenterImpl(BriteDatabase briteDatabase, D2 d2) {
        this.briteDatabase = briteDatabase;
        this.d2 = d2;
        this.compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void handleEventWORegistrationInfo(JSONObject jsonObject) {
        this.eventWORegistrationJson = jsonObject;
        eventUid = null;
        try {
            eventUid = jsonObject.getString("uid");
        } catch (JSONException e) {
            Timber.e(e);
        }

        view.renderEventWORegistrationInfo(eventUid);
    }

    private TrackedEntityDataValue.Builder getTeiDataValue(JSONObject attrValue, SimpleDateFormat simpleDateFormat, boolean useEventUid) throws JSONException, ParseException {
        TrackedEntityDataValue.Builder trackedEntityDataValueModelBuilder = TrackedEntityDataValue.builder();

        if (useEventUid) {
            trackedEntityDataValueModelBuilder.event(eventUid);
        } else {
            if (attrValue.has(EVENT)) {
                trackedEntityDataValueModelBuilder.event(attrValue.getString(EVENT));
            }
        }

        if (attrValue.has(DATA_ELEMENT)) {
            trackedEntityDataValueModelBuilder.dataElement(attrValue.getString(DATA_ELEMENT));
        }
        if (attrValue.has(STORED_BY)) {
            trackedEntityDataValueModelBuilder.storedBy(attrValue.getString(STORED_BY));
        }
        if (attrValue.has(VALUE)) {
            trackedEntityDataValueModelBuilder.value(attrValue.getString(VALUE));
        }
        if (attrValue.has(PROVIDED_ELSEWHERE)) {
            trackedEntityDataValueModelBuilder.providedElsewhere(Boolean.parseBoolean(attrValue.getString(PROVIDED_ELSEWHERE)));
        }
        if (attrValue.has(CREATED)) {
            trackedEntityDataValueModelBuilder.created(simpleDateFormat.parse(attrValue.getString(CREATED)));
        }
        if (attrValue.has(LAST_UPDATED)) {
            trackedEntityDataValueModelBuilder.lastUpdated(simpleDateFormat.parse(attrValue.getString(LAST_UPDATED)));
        }
        return trackedEntityDataValueModelBuilder;
    }

    private ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> getTeiAttributes(JSONObject attrValue, TrackedEntityDataValue.Builder trackedEntityDataValueModelBuilder) throws JSONException {
        ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> attributes = new ArrayList<>();
        if (attrValue.has(DATA_ELEMENT) && attrValue.getString(DATA_ELEMENT) != null) {
            // LOOK FOR dataElement ON LOCAL DATABASE.
            // IF FOUND, OPEN DASHBOARD
            try (Cursor cursor = briteDatabase.query(SELECT_ALL_FROM + SqlConstants.DATA_ELEMENT_TABLE +
                    WHERE + SqlConstants.DATA_ELEMENT_UID + " = ?", attrValue.getString(DATA_ELEMENT))) {
                if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                    this.dataJson.add(attrValue);
                    attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), cursor.getString(cursor.getColumnIndex("formName")), true));
                } else {
                    attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), null, false));
                }
            }
        } else {
            attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), null, false));
        }
        return attributes;
    }


    @Override
    public void handleDataWORegistrationInfo(JSONArray jsonArray) {
        ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> attributes = new ArrayList<>();
        if (eventUid != null) {
            try {
                // LOOK FOR TRACKED ENTITY ATTRIBUTES ON LOCAL DATABASE
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject attrValue = jsonArray.getJSONObject(i);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATABASE_FORMAT_EXPRESSION, Locale.getDefault());
                    TrackedEntityDataValue.Builder trackedEntityDataValueModelBuilder = getTeiDataValue(attrValue, simpleDateFormat, true);
                    attributes.addAll(getTeiAttributes(attrValue, trackedEntityDataValueModelBuilder));
                }
            } catch (JSONException | ParseException e) {
                Timber.e(e);
            }
        }

        view.renderEventDataInfo(attributes);
    }

    private ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> getTeiDataAttributes(JSONObject attrValue,
                                                                                          TrackedEntityDataValue.Builder trackedEntityDataValueModelBuilder) throws JSONException {
        ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> attributes = new ArrayList<>();
        if (attrValue.has(DATA_ELEMENT) && attrValue.getString(DATA_ELEMENT) != null) {
            // LOOK FOR dataElement ON LOCAL DATABASE.
            // IF FOUND, OPEN DASHBOARD
            try (Cursor cursor = briteDatabase.query(SELECT_ALL_FROM + SqlConstants.DATA_ELEMENT_TABLE +
                    WHERE + SqlConstants.DATA_ELEMENT_UID + " = ?", attrValue.getString(DATA_ELEMENT))) {
                if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                    this.teiDataJson.add(attrValue);
                    attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), cursor.getString(cursor.getColumnIndex("formName")), true));
                } else {
                    attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), null, false));
                }
            }
        } else {
            attributes.add(Trio.create(trackedEntityDataValueModelBuilder.build(), null, false));
        }
        return attributes;
    }

    @Override
    public void handleDataInfo(JSONArray jsonArray) {
        ArrayList<Trio<TrackedEntityDataValue, String, Boolean>> attributes = new ArrayList<>();
        try {
            // LOOK FOR TRACKED ENTITY ATTRIBUTES ON LOCAL DATABASE
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject attrValue = jsonArray.getJSONObject(i);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATABASE_FORMAT_EXPRESSION, Locale.getDefault());
                TrackedEntityDataValue.Builder trackedEntityDataValueModelBuilder = getTeiDataValue(attrValue, simpleDateFormat, false);
                attributes.addAll(getTeiDataAttributes(attrValue, trackedEntityDataValueModelBuilder));
            }
        } catch (JSONException | ParseException e) {
            Timber.e(e);
        }

        view.renderTeiEventDataInfo(attributes);
    }

    @Override
    public void handleTeiInfo(JSONObject jsonObject) {
        this.teiJson = jsonObject;
        teiUid = null;
        try {
            teiUid = jsonObject.getString("uid");
        } catch (JSONException e) {
            Timber.e(e);
        }

        // IF TEI READ
        if (teiUid != null) {
            // LOOK FOR TEI ON LOCAL DATABASE.
            try (Cursor cursor = briteDatabase.query(SELECT_ALL_FROM + SqlConstants.TEI_TABLE +
                    WHERE + SqlConstants.TEI_UID + " = ?", teiUid)) {
                // IF FOUND, OPEN DASHBOARD
                if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                    view.goToDashBoard(teiUid);
                }
                // IF NOT FOUND, TRY TO DOWNLOAD ONLINE, OR PROMPT USER TO SCAN MORE QR CODES
                else {
                    view.downloadTei(teiUid);
                }
            }
        }
        // IF NO TEI PRESENT ON THE QR, SHOW ERROR
        else
            view.renderTeiInfo(null);
    }

    @Override
    public void handleAttrInfo(JSONArray jsonArray) {
        this.attrJson.add(jsonArray);
        ArrayList<Trio<String, String, Boolean>> attributes = new ArrayList<>();
        try {
            // LOOK FOR TRACKED ENTITY ATTRIBUTES ON LOCAL DATABASE
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject attrValue = jsonArray.getJSONObject(i);
                if (attrValue.has(TE_ATTR) && attrValue.getString(TE_ATTR) != null) {
                    try (Cursor cursor = briteDatabase.query(SELECT +
                                    SqlConstants.TE_ATTR_UID + ", " +
                                    SqlConstants.TE_ATTR_DISPLAY_NAME +
                                    FROM + SqlConstants.TE_ATTR_TABLE +
                                    WHERE + SqlConstants.TE_ATTR_UID + " = ?",
                            attrValue.getString(TE_ATTR))) {
                        // TRACKED ENTITY ATTRIBUTE FOUND, TRACKED ENTITY ATTRIBUTE VALUE CAN BE SAVED.
                        if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                            attributes.add(Trio.create(cursor.getString(1), attrValue.getString(VALUE), true));
                        }
                        // TRACKED ENTITY ATTRIBUTE NOT FOUND, TRACKED ENTITY ATTRIBUTE VALUE CANNOT BE SAVED.
                        else {
                            attributes.add(Trio.create(attrValue.getString(TE_ATTR), "", false));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        view.renderAttrInfo(attributes);
    }

    @Override
    public void handleEnrollmentInfo(JSONArray jsonArray) {
        this.enrollmentJson.add(jsonArray);
        ArrayList<Pair<String, Boolean>> enrollments = new ArrayList<>();
        try {
            // LOOK FOR PROGRAM ON LOCAL DATABASE
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject attrValue = jsonArray.getJSONObject(i);
                if (attrValue.has(PROGRAM) && attrValue.getString(PROGRAM) != null) {
                    try (Cursor cursor = briteDatabase.query(SELECT +
                                    SqlConstants.PROGRAM_UID + ", " +
                                    SqlConstants.PROGRAM_DISPLAY_NAME +
                                    FROM + SqlConstants.PROGRAM_TABLE +
                                    WHERE + SqlConstants.PROGRAM_UID + " = ?",
                            attrValue.getString(PROGRAM))) {
                        // PROGRAM FOUND, ENROLLMENT CAN BE SAVED
                        if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                            enrollments.add(Pair.create(cursor.getString(1), true));
                        }
                        // PROGRAM NOT FOUND, ENROLLMENT CANNOT BE SAVED
                        else {
                            enrollments.add(Pair.create(attrValue.getString("uid"), false));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        view.renderEnrollmentInfo(enrollments);
    }


    @Override
    public void handleEventInfo(JSONObject jsonObject) {
        this.eventsJson.add(jsonObject);
        ArrayList<Pair<String, Boolean>> events = new ArrayList<>();
        try {
            // LOOK FOR ENROLLMENT ON LOCAL DATABASE
            if (jsonObject.has(ENROLLMENT) && jsonObject.getString(ENROLLMENT) != null) {
                parseEvents(jsonObject);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        view.renderEventInfo(events);
    }

    private ArrayList<Pair<String, Boolean>> parseEvents(JSONObject jsonObject) throws JSONException {
        ArrayList<Pair<String, Boolean>> events = new ArrayList<>();
        try (Cursor cursor = briteDatabase.query(SELECT +
                        SqlConstants.ENROLLMENT_UID +
                        FROM + SqlConstants.ENROLLMENT_TABLE +
                        WHERE + SqlConstants.ENROLLMENT_UID + " = ?",
                jsonObject.getString(ENROLLMENT))) {
            // ENROLLMENT FOUND, EVENT CAN BE SAVED
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                events.add(Pair.create(jsonObject.getString(ENROLLMENT), true));
            }
            // ENROLLMENT NOT FOUND IN LOCAL DATABASE, CHECK IF IT WAS READ FROM A QR
            else if (enrollmentJson != null) {
                events.add(Pair.create(jsonObject.getString("uid"), isEnrollmentReadFromQr(jsonObject)));
            }
            // ENROLLMENT NOT FOUND, EVENT CANNOT BE SAVED
            else {
                events.add(Pair.create(jsonObject.getString("uid"), false));
            }
        }
        return events;
    }

    private boolean isEnrollmentReadFromQr(JSONObject jsonObject) throws JSONException {
        boolean isEnrollmentReadFromQr = false;
        for (int i = 0; i < enrollmentJson.size(); i++) {
            JSONArray enrollmentArray = enrollmentJson.get(i);
            for (int j = 0; j < enrollmentArray.length(); j++) {
                JSONObject enrollment = enrollmentArray.getJSONObject(j);
                if (jsonObject.getString(ENROLLMENT).equals(enrollment.getString(SqlConstants.ENROLLMENT_UID))) {
                    isEnrollmentReadFromQr = true;
                    break;
                }
            }
        }
        return isEnrollmentReadFromQr;
    }

    @Override
    public void handleRelationship(JSONArray jsonArray) {
        this.relationshipsJson.add(jsonArray);
        ArrayList<Pair<String, Boolean>> relationships = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject relationship = jsonArray.getJSONObject(i);
                relationships.add(Pair.create(relationship.getString("trackedEntityInstanceA"), true));
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        view.renderRelationship(relationships);
    }

    @Override
    public void init(QrReaderContracts.View view) {
        this.view = view;
    }

    private TrackedEntityInstance createTei() throws JSONException, ParseException {
        TrackedEntityInstance.Builder teiModelBuilder = TrackedEntityInstance.builder();
        if (teiJson.has("uid"))
            teiModelBuilder.uid(teiJson.getString("uid"));
        if (teiJson.has(CREATED))
            teiModelBuilder.created(DateUtils.databaseDateFormat().parse(teiJson.getString(CREATED)));
        if (teiJson.has(LAST_UPDATED))
            teiModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(teiJson.getString(LAST_UPDATED)));
        if (teiJson.has(STATE))
            teiModelBuilder.state(State.valueOf(teiJson.getString(STATE)));
        if (teiJson.has(ORG_UNIT))
            teiModelBuilder.organisationUnit(teiJson.getString(ORG_UNIT));
        if (teiJson.has("trackedEntityType"))
            teiModelBuilder.trackedEntityType(teiJson.getString("trackedEntityType"));

        return teiModelBuilder.build();
    }

    private void downloadTei() {
        try {
            if (teiJson != null) {
                TrackedEntityInstance teiModel = createTei();
                if (teiModel != null)
                    briteDatabase.insert(SqlConstants.TEI_TABLE, teiModel.toContentValues());
            } else {
                view.showIdError();
            }
        } catch (JSONException | ParseException e) {
            Timber.e(e);
        }
    }

    private TrackedEntityAttributeValue createTeAttrValue(JSONObject attrV) throws JSONException, ParseException {
        TrackedEntityAttributeValue.Builder attrValueModelBuilder;
        attrValueModelBuilder = TrackedEntityAttributeValue.builder();
        if (attrV.has(CREATED))
            attrValueModelBuilder.created(DateUtils.databaseDateFormat().parse(attrV.getString(CREATED)));
        if (attrV.has(LAST_UPDATED))
            attrValueModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(attrV.getString(LAST_UPDATED)));
        if (attrV.has(VALUE))
            attrValueModelBuilder.value(attrV.getString(VALUE));
        if (attrV.has(TE_INSTANCE))
            attrValueModelBuilder.trackedEntityInstance(attrV.getString(TE_INSTANCE));
        if (attrV.has(TE_ATTR))
            attrValueModelBuilder.trackedEntityAttribute(attrV.getString(TE_ATTR));

        return attrValueModelBuilder.build();
    }

    private void downloadAttr() {
        if (attrJson != null) {
            for (int i = 0; i < attrJson.size(); i++) {
                JSONArray attrArray = attrJson.get(i);
                for (int j = 0; j < attrArray.length(); j++) {
                    try {
                        JSONObject attrV = attrArray.getJSONObject(j);
                        TrackedEntityAttributeValue attrValueModel = createTeAttrValue(attrV);
                        if (attrValueModel != null)
                            briteDatabase.insert(SqlConstants.TE_ATTR_VALUE_TABLE, attrValueModel.toContentValues());

                    } catch (JSONException | ParseException e) {
                        Timber.e(e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("squid:CommentedOutCodeLine")
    private void downloadRelationships() {
        if (relationshipsJson != null) {
            for (int i = 0; i < relationshipsJson.size(); i++) {
                //TODO: CHANGE RELATIONSHIPS
            /*try {
                JSONObject relationship = relationshipsJson.getJSONObject(i);


                RelationshipModel.Builder relationshipModelBuilder;
                relationshipModelBuilder = RelationshipModel.builder();

                if (relationship.has("trackedEntityInstanceA"))
                    relationshipModelBuilder.trackedEntityInstanceA(relationship.getString("trackedEntityInstanceA"));
                if (relationship.has("trackedEntityInstanceB"))
                    relationshipModelBuilder.trackedEntityInstanceB(relationship.getString("trackedEntityInstanceB"));
                if (relationship.has("relationshipType"))
                    relationshipModelBuilder.relationshipType(relationship.getString("relationshipType"));

                RelationshipModel relationshipModel = relationshipModelBuilder.build();

                if (relationshipModel != null)
                    briteDatabase.insert(RelationshipModel.TABLE, relationshipModel.toContentValues());

            } catch (Exception e) {
                Timber.e(e);
            }*/
            }
        }
    }

    private Enrollment createEnrollment(JSONObject enrollment) throws JSONException, ParseException {
        Enrollment.Builder enrollmentModelBuilder;
        enrollmentModelBuilder = Enrollment.builder();
        if (enrollment.has("uid"))
            enrollmentModelBuilder.uid(enrollment.getString("uid"));
        if (enrollment.has(CREATED))
            enrollmentModelBuilder.created(DateUtils.databaseDateFormat().parse(enrollment.getString(CREATED)));
        if (enrollment.has(LAST_UPDATED))
            enrollmentModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(enrollment.getString(LAST_UPDATED)));
        if (enrollment.has(STATE))
            enrollmentModelBuilder.state(State.valueOf(enrollment.getString(STATE)));
        if (enrollment.has(PROGRAM))
            enrollmentModelBuilder.program(enrollment.getString(PROGRAM));
        if (enrollment.has("followUp"))
            enrollmentModelBuilder.followUp(enrollment.getBoolean("followUp"));
        if (enrollment.has("enrollmentStatus"))
            enrollmentModelBuilder.status(EnrollmentStatus.valueOf(enrollment.getString("enrollmentStatus")));
        if (enrollment.has("enrollmentDate"))
            enrollmentModelBuilder.enrollmentDate(DateUtils.databaseDateFormat().parse(enrollment.getString("enrollmentDate")));
        if (enrollment.has("dateOfIncident"))
            enrollmentModelBuilder.incidentDate(DateUtils.databaseDateFormat().parse(enrollment.getString("incidentDate ")));
        if (enrollment.has(ORG_UNIT))
            enrollmentModelBuilder.organisationUnit(enrollment.getString(ORG_UNIT));
        if (enrollment.has(TE_INSTANCE))
            enrollmentModelBuilder.trackedEntityInstance(enrollment.getString(TE_INSTANCE));

        return enrollmentModelBuilder.build();
    }

    private void downloadEnrollments() {
        if (enrollmentJson != null) {
            for (int i = 0; i < enrollmentJson.size(); i++) {
                JSONArray enrollmentArray = enrollmentJson.get(i);
                for (int j = 0; j < enrollmentArray.length(); j++) {
                    try {
                        JSONObject enrollmentJsonResult = enrollmentArray.getJSONObject(j);
                        Enrollment enrollmentModel = createEnrollment(enrollmentJsonResult);

                        if (enrollmentModel != null)
                            briteDatabase.insert(SqlConstants.ENROLLMENT_TABLE, enrollmentModel.toContentValues());

                    } catch (JSONException | ParseException e) {
                        Timber.e(e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("squid:S3776")
    private Event createEvent(JSONObject event) throws JSONException, ParseException {
        Event.Builder eventModelBuilder;
        eventModelBuilder = Event.builder();
        if (event.has("uid"))
            eventModelBuilder.uid(event.getString("uid"));
        if (event.has(CREATED))
            eventModelBuilder.created(DateUtils.databaseDateFormat().parse(event.getString(CREATED)));
        if (event.has(LAST_UPDATED))
            eventModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(event.getString(LAST_UPDATED)));
        if (event.has(STATE))
            eventModelBuilder.state(State.valueOf(event.getString(STATE)));
        if (event.has(ENROLLMENT))
            eventModelBuilder.enrollment(event.getString(ENROLLMENT));
        if (event.has(PROGRAM))
            eventModelBuilder.program(event.getString(PROGRAM));
        if (event.has(PROGRAM_STAGE))
            eventModelBuilder.programStage(event.getString(PROGRAM_STAGE));
        if (event.has(ORG_UNIT))
            eventModelBuilder.organisationUnit(event.getString(ORG_UNIT));
        if (event.has(EVENT_DATE))
            eventModelBuilder.eventDate(DateUtils.databaseDateFormat().parse(event.getString(EVENT_DATE)));
        if (event.has(STATUS))
            eventModelBuilder.status(EventStatus.valueOf(event.getString(STATUS)));
        if (event.has(ATTR_OPTION_COMBO))
            eventModelBuilder.attributeOptionCombo(event.getString(ATTR_OPTION_COMBO));
        if (event.has(TE_INSTANCE))
            eventModelBuilder.trackedEntityInstance(event.getString(TE_INSTANCE));
        String lat = "0.0";
        String lon = "0.0";
        if (event.has(LATITUDE))
            lat = event.getString(LATITUDE);
        if (event.has(LONGITUDE))
            lon = event.getString(LONGITUDE);

        Coordinates coordinates = Coordinates.create(Double.valueOf(lat), Double.valueOf(lon));
        eventModelBuilder.coordinate(coordinates);

        if (event.has(COMPLETED_DATE))
            eventModelBuilder.completedDate(DateUtils.databaseDateFormat().parse(event.getString(COMPLETED_DATE)));
        if (event.has(DUE_DATE))
            eventModelBuilder.dueDate(DateUtils.databaseDateFormat().parse(event.getString(DUE_DATE)));

        return eventModelBuilder.build();
    }

    private void downloadEvents() {
        if (eventsJson != null) {
            for (int i = 0; i < eventsJson.size(); i++) {
                try {
                    JSONObject event = eventsJson.get(i);
                    Event eventModel = createEvent(event);

                    if (eventModel != null)
                        briteDatabase.insert(SqlConstants.EVENT_TABLE, eventModel.toContentValues());

                } catch (JSONException | ParseException e) {
                    Timber.e(e);
                }
            }
        }
    }

    private TrackedEntityDataValue createTeiDataValue(JSONObject attrV) throws JSONException, ParseException {
        TrackedEntityDataValue.Builder attrValueModelBuilder;
        attrValueModelBuilder = TrackedEntityDataValue.builder();

        if (attrV.has(EVENT))
            attrValueModelBuilder.event(attrV.getString(EVENT));
        if (attrV.has(LAST_UPDATED))
            attrValueModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(attrV.getString(LAST_UPDATED)));
        if (attrV.has(DATA_ELEMENT))
            attrValueModelBuilder.dataElement(attrV.getString(DATA_ELEMENT));
        if (attrV.has(STORED_BY))
            attrValueModelBuilder.storedBy(attrV.getString(STORED_BY));
        if (attrV.has(VALUE))
            attrValueModelBuilder.value(attrV.getString(VALUE));
        if (attrV.has(PROVIDED_ELSEWHERE))
            attrValueModelBuilder.providedElsewhere(Boolean.parseBoolean(attrV.getString(PROVIDED_ELSEWHERE)));

        return attrValueModelBuilder.build();
    }

    private void downloadTeiData() {
        for (int i = 0; i < teiDataJson.size(); i++) {
            try {
                JSONObject attrV = teiDataJson.get(i);
                TrackedEntityDataValue attrValueModel = createTeiDataValue(attrV);

                if (attrValueModel != null) {
                    long result = briteDatabase.insert(SqlConstants.TEI_DATA_VALUE_TABLE, attrValueModel.toContentValues());
                    Timber.d(LOG_INSERT, result);
                }

            } catch (JSONException | ParseException e) {
                Timber.e(e);
            }
        }
    }

    @SuppressWarnings("squid:CommentedOutCodeLine")
    // SAVES READ TRACKED ENTITY INSTANCE, TRACKED ENTITY ATTRIBUTE VALUES, ENROLLMENTS, EVENTS AND RELATIONSHIPS INTO LOCAL DATABASE
    @Override
    public void download() {
        downloadTei();
        downloadAttr();
        downloadRelationships();
        downloadEnrollments();
        downloadEvents();
        downloadTeiData();
        view.goToDashBoard(teiUid);
    }


    // CALLS THE ENDOPOINT TO DOWNLOAD AND SAVE THE TRACKED ENTITY INSTANCE INFO
    @Override
    public void onlineDownload() {
        view.initDownload();
        List<String> uidToDownload = new ArrayList<>();
        uidToDownload.add(teiUid);
        compositeDisposable.add(
                Observable.defer(() -> Observable.fromCallable(d2.trackedEntityModule().downloadTrackedEntityInstancesByUid(uidToDownload))).toFlowable(BackpressureStrategy.LATEST)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    view.finishDownload();
                                    if (data != null && !data.isEmpty()) {
                                        this.teiUid = data.get(0).uid();
                                        view.goToDashBoard(data.get(0).uid());
                                    } else {
                                        view.renderTeiInfo(teiUid);
                                    }
                                },
                                error -> {
                                    view.finishDownload();
                                    view.renderTeiInfo(teiUid);
                                }
                        )
        );
    }

    @Override
    public void dispose() {
        compositeDisposable.clear();
    }

    @SuppressWarnings("squid:S3776")
    private Event createEventWORegistration() throws JSONException, ParseException {
        Event.Builder eventModelBuilder = Event.builder();
        if (eventWORegistrationJson.has("uid")) {
            eventModelBuilder.uid(eventWORegistrationJson.getString("uid"));
        }
        if (eventWORegistrationJson.has(ENROLLMENT)) {
            eventModelBuilder.enrollment(eventWORegistrationJson.getString(ENROLLMENT));
        }
        if (eventWORegistrationJson.has(CREATED)) {
            eventModelBuilder.created(DateUtils.databaseDateFormat().parse(eventWORegistrationJson.getString(CREATED)));
        }
        if (eventWORegistrationJson.has(LAST_UPDATED)) {
            eventModelBuilder.lastUpdated(DateUtils.databaseDateFormat().parse(eventWORegistrationJson.getString(LAST_UPDATED)));
        }
        if (eventWORegistrationJson.has("createdAtClient")) {
            eventModelBuilder.createdAtClient(eventWORegistrationJson.getString("createdAtClient"));
        }
        if (eventWORegistrationJson.has("lastUpdatedAtClient")) {
            eventModelBuilder.lastUpdatedAtClient(eventWORegistrationJson.getString("lastUpdatedAtClient"));
        }
        if (eventWORegistrationJson.has(STATUS)) {
            eventModelBuilder.status(EventStatus.valueOf(eventWORegistrationJson.getString(STATUS)));
        }

        String lat = "0.0";
        String lon = "0.0";
        if (eventWORegistrationJson.has(LATITUDE))
            lat = eventWORegistrationJson.getString(LATITUDE);
        if (eventWORegistrationJson.has(LONGITUDE))
            lon = eventWORegistrationJson.getString(LONGITUDE);

        Coordinates coordinates = Coordinates.create(Double.valueOf(lat), Double.valueOf(lon));
        eventModelBuilder.coordinate(coordinates);

        if (eventWORegistrationJson.has(PROGRAM)) {
            eventModelBuilder.program(eventWORegistrationJson.getString(PROGRAM));
        }
        if (eventWORegistrationJson.has(PROGRAM_STAGE)) {
            eventModelBuilder.programStage(eventWORegistrationJson.getString(PROGRAM_STAGE));
        }
        if (eventWORegistrationJson.has(PROGRAM_STAGE)) {
            eventModelBuilder.programStage(eventWORegistrationJson.getString(PROGRAM_STAGE));
        }
        if (eventWORegistrationJson.has(ORG_UNIT)) {
            eventModelBuilder.organisationUnit(eventWORegistrationJson.getString(ORG_UNIT));
        }
        if (eventWORegistrationJson.has(EVENT_DATE)) {
            eventModelBuilder.eventDate(DateUtils.databaseDateFormat().parse(eventWORegistrationJson.getString(EVENT_DATE)));
        }
        if (eventWORegistrationJson.has(COMPLETED_DATE)) {
            eventModelBuilder.completedDate(DateUtils.databaseDateFormat().parse(eventWORegistrationJson.getString(COMPLETED_DATE)));
        }
        if (eventWORegistrationJson.has(DUE_DATE)) {
            eventModelBuilder.dueDate(DateUtils.databaseDateFormat().parse(eventWORegistrationJson.getString(DUE_DATE)));
        }
        if (eventWORegistrationJson.has(ATTR_OPTION_COMBO)) {
            eventModelBuilder.attributeOptionCombo(eventWORegistrationJson.getString(ATTR_OPTION_COMBO));
        }
        if (eventWORegistrationJson.has(TE_INSTANCE)) {
            eventModelBuilder.trackedEntityInstance(eventWORegistrationJson.getString(TE_INSTANCE));
        }

        eventModelBuilder.state(State.TO_UPDATE);

        return eventModelBuilder.build();
    }

    private void insertEventWORegistration(Event eventModel) {
        try (Cursor cursor = briteDatabase.query(SELECT_ALL_FROM + SqlConstants.EVENT_TABLE +
                WHERE + SqlConstants.EVENT_UID + " = ?", eventModel.uid())) {
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                // EVENT ALREADY EXISTS IN THE DATABASE, JUST INSERT ATTRIBUTES
            } else {
                long result = briteDatabase.insert(SqlConstants.EVENT_TABLE, eventModel.toContentValues());
                Timber.d(LOG_INSERT, result);
            }
        }
    }

    @Override
    public void downloadEventWORegistration() {

        String programUid = null;
        String orgUnit = null;

        try {
            if (eventWORegistrationJson != null) {
                Event eventModel = createEventWORegistration();
                orgUnit = eventModel.organisationUnit() == null ? "" : eventModel.organisationUnit();
                programUid = eventModel.program() == null ? "" : eventModel.program();
                insertEventWORegistration(eventModel);

            } else {
                view.showIdError();
                return;
            }
        } catch (JSONException | ParseException e) {
            Timber.e(e);
        }

        downloadTeiDataValues();

        view.goToEvent(eventUid, programUid, orgUnit);
    }

    private void downloadTeiDataValues() {
        for (int i = 0; i < dataJson.size(); i++) {
            try {
                JSONObject attrV = dataJson.get(i);

                TrackedEntityDataValue attrValueModel = createTeiDataValue(attrV);

                if (attrValueModel != null) {
                    long result = briteDatabase.insert(SqlConstants.TEI_DATA_VALUE_TABLE, attrValueModel.toContentValues());
                    Timber.d(LOG_INSERT, result);
                }

            } catch (JSONException | ParseException e) {
                Timber.e(e);
            }
        }
    }
}
