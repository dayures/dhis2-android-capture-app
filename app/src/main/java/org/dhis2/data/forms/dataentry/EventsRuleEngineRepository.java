package org.dhis2.data.forms.dataentry;

import androidx.annotation.NonNull;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.forms.FormRepository;
import org.dhis2.data.forms.RuleHelper;
import org.dhis2.utils.Result;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEvent;

import java.util.Arrays;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;


public final class EventsRuleEngineRepository implements RuleEngineRepository {
    private static final String QUERY_EVENT = "SELECT Event.uid,\n" +
            "  Event.programStage,\n" +
            "  Event.status,\n" +
            "  Event.eventDate,\n" +
            "  Event.dueDate,\n" +
            "  Event.organisationUnit,\n" +
            "  ProgramStage.displayName\n" +
            "FROM Event\n" +
            "JOIN ProgramStage ON ProgramStage.uid = Event.programStage\n" +
            "WHERE Event.uid = ?\n" +
            " AND " + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_STATE + " != '" + State.TO_DELETE + "'" +
            "LIMIT 1;";

    private static final String QUERY_VALUES = "SELECT " +
            "  Event.eventDate," +
            "  Event.programStage," +
            "  TrackedEntityDataValue.dataElement," +
            "  TrackedEntityDataValue.value," +
            "  ProgramRuleVariable.useCodeForOptionSet," +
            "  Option.code," +
            "  Option.name" +
            " FROM TrackedEntityDataValue " +
            "  INNER JOIN Event ON TrackedEntityDataValue.event = Event.uid " +
            "  INNER JOIN DataElement ON DataElement.uid = TrackedEntityDataValue.dataElement " +
            "  LEFT JOIN ProgramRuleVariable ON ProgramRuleVariable.dataElement = DataElement.uid " +
            "  LEFT JOIN Option ON (Option.optionSet = DataElement.optionSet AND Option.code = TrackedEntityDataValue.value) " +
            " WHERE Event.uid = ? AND value IS NOT NULL AND " + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_STATE + " != '" + State.TO_DELETE + "';";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final FormRepository formRepository;

    @NonNull
    private final String eventUid;

    public EventsRuleEngineRepository(@NonNull BriteDatabase briteDatabase,
                                      @NonNull FormRepository formRepository, @NonNull String eventUid) {
        this.briteDatabase = briteDatabase;
        this.formRepository = formRepository;
        this.eventUid = eventUid;
    }

    @Override
    public void updateRuleAttributeMap(String uid, String value) {
        // unused
    }

    @Override
    public Flowable<RuleEngine> updateRuleEngine() {
        return formRepository.restartRuleEngine();
    }

    @NonNull
    @Override
    public Flowable<Result<RuleEffect>> calculate() {
        return queryDataValues()
                .switchMap(this::queryEvent)
                .switchMap(event -> formRepository.ruleEngine()
                        .switchMap(ruleEngine -> Flowable.fromCallable(ruleEngine.evaluate(event))
                                .map(Result::success)
                                .onErrorReturn(error -> Result.failure(new Exception(error)))
                        )
                );
    }

    @NonNull
    @Override
    public Flowable<Result<RuleEffect>> reCalculate() {
        return calculate();
    }

    @NonNull
    private Flowable<RuleEvent> queryEvent(@NonNull List<RuleDataValue> dataValues) {
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, QUERY_EVENT, eventUid)
                .mapToOne(cursor -> RuleHelper.createRuleEventFromCursor(briteDatabase, cursor, dataValues))
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    private Flowable<List<RuleDataValue>> queryDataValues() {
        return briteDatabase.createQuery(Arrays.asList(SqlConstants.EVENT_TABLE,
                SqlConstants.TEI_DATA_VALUE_TABLE), QUERY_VALUES, eventUid)
                .mapToList(RuleHelper::createRuleDataValue).toFlowable(BackpressureStrategy.LATEST);
    }
}
