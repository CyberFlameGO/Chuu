package core.parsers;

import dao.ChuuService;
import dao.entities.NaturalTimeFrameEnum;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class PaceParser extends NumberParser<ExtraParser<NaturalTimeFrameParser, Long>> {

    private final static LocalDate LASTFM_CREATION_DATE = LocalDate.of(2002, 2, 20);

    // Dont ask
    public PaceParser(ChuuService dao, Map<Integer, String> errorMessages, String fieldName, String fieldDescription) {
        super(new ExtraParser<>(
                        new NaturalTimeFrameParser(dao, NaturalTimeFrameEnum.ALL),
                        null,
                        NumberParser.predicate,
                        (x) -> false,
                        Long::parseLong,
                        String::valueOf,
                        errorMessages,
                        fieldName,
                        fieldDescription,
                        (arr, number) -> {
                            NaturalTimeFrameEnum naturalTimeFrameEnum = NaturalTimeFrameEnum.fromCompletePeriod(arr[2]);
                            LocalDate now = LocalDate.now();
                            return switch (naturalTimeFrameEnum) {
                                case YEAR -> number > (int) ChronoUnit.YEARS.between(LASTFM_CREATION_DATE, now);
                                case QUARTER -> number > (int) ChronoUnit.YEARS.between(LASTFM_CREATION_DATE, now) * 4;
                                case MONTH -> number > ChronoUnit.MONTHS.between(LASTFM_CREATION_DATE, now);
                                case ALL -> false;
                                case SEMESTER -> number > ChronoUnit.YEARS.between(LASTFM_CREATION_DATE, now) * 2;
                                case WEEK -> number > ChronoUnit.WEEKS.between(LASTFM_CREATION_DATE, now);
                                case DAY -> number > ChronoUnit.DAYS.between(LASTFM_CREATION_DATE, now);
                            };
                        })
                , null, Long.MAX_VALUE, new HashMap<>(), fieldDescription, false, (list) -> list.stream().mapToLong(Long::longValue).max().getAsLong());
    }
    // [0] -> NumberParserResult [1] -> ExtraParser of Natural
    //
}
