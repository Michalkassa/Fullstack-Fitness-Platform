package com.lockedin.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.ActivityDay;
import com.lockedin.api.dto.PersonalRecord;
import com.lockedin.api.dto.WeeklyStat;

/** docs/API.md, "Analytics": endpoints 34–36, all calculated from the user's logs ("Business rules"). */
@RestController
@RequestMapping("/api/analytics")
class AnalyticsController {

    /** 34. Volume and training days per week, oldest first; only weeks with logs. */
    @GetMapping("/weekly")
    List<WeeklyStat> weekly() {
        throw new NotImplementedException();
    }

    /** 35. Sets per day, only days with at least one set, across all years. */
    @GetMapping("/activity")
    List<ActivityDay> activity() {
        throw new NotImplementedException();
    }

    /** 36. Best results per logged exercise, sorted by exercise name. */
    @GetMapping("/records")
    List<PersonalRecord> records() {
        throw new NotImplementedException();
    }
}
