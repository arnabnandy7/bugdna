from __future__ import annotations

from collections import deque
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
import threading
from typing import Iterable, Optional, Union

from ._bugdna import generate
from ._family import FailureFamily
from ._fingerprint import Fingerprint

_DEFAULT_TOP_FAILURE_LIMIT = 10
_DEFAULT_TOP_FAMILY_LIMIT = 10
_DEFAULT_TIMELINE_LIMIT = 10_000
_DEFAULT_BURST_MAX_IDLE_GAP = timedelta(minutes=1)


def _to_utc_datetime_and_epoch(
    value: Optional[Union[datetime, int, float]] = None,
) -> tuple[datetime, int]:
    if value is None:
        dt = datetime.now(timezone.utc)
        return dt, int(dt.timestamp())
    if isinstance(value, (int, float)):
        sec = int(value)
        return datetime.fromtimestamp(sec, tz=timezone.utc), sec
    if isinstance(value, datetime):
        if value.tzinfo is None:
            dt = value.replace(tzinfo=timezone.utc)
        else:
            dt = value.astimezone(timezone.utc)
        return dt, int(dt.timestamp())
    raise TypeError("occurred_at must be datetime, int, float, or None")


def _format_utc_time(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%H:%M")


@dataclass(frozen=True)
class FailureOccurrence:
    occurred_at: datetime
    epoch_second: int
    fingerprint: Fingerprint
    id: str

    def __init__(
        self,
        occurred_at: Union[datetime, int, float],
        fingerprint: Fingerprint,
    ) -> None:
        if occurred_at is None:
            raise ValueError("occurred_at must not be None")
        if fingerprint is None:
            raise ValueError("fingerprint must not be None")
        dt, sec = _to_utc_datetime_and_epoch(occurred_at)
        object.__setattr__(self, "occurred_at", dt)
        object.__setattr__(self, "epoch_second", sec)
        object.__setattr__(self, "fingerprint", fingerprint)
        object.__setattr__(self, "id", fingerprint.id)

    @property
    def occurredAt(self) -> datetime:
        return self.occurred_at

    def get_occurred_at(self) -> datetime:
        return self.occurred_at

    def getOccurredAt(self) -> datetime:
        return self.occurred_at

    def get_fingerprint(self) -> Fingerprint:
        return self.fingerprint

    def getFingerprint(self) -> Fingerprint:
        return self.fingerprint

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id


@dataclass(frozen=True)
class FailureBurst:
    fingerprint: Fingerprint
    id: str
    first_seen: datetime
    last_seen: datetime
    peak_rate_per_minute: int
    occurrences: int
    duration_seconds: int

    def __init__(
        self,
        fingerprint: Fingerprint,
        first_seen: datetime,
        last_seen: datetime,
        peak_rate_per_minute: int,
        occurrences: int,
    ) -> None:
        if fingerprint is None:
            raise ValueError("fingerprint must not be None")
        if first_seen is None or last_seen is None:
            raise ValueError("timestamps must not be None")
        if last_seen < first_seen:
            raise ValueError("last_seen must not be before first_seen")
        if peak_rate_per_minute < 1:
            raise ValueError("peak_rate_per_minute must be at least 1")
        if occurrences < 1:
            raise ValueError("occurrences must be at least 1")

        dur = int((last_seen - first_seen).total_seconds())
        object.__setattr__(self, "fingerprint", fingerprint)
        object.__setattr__(self, "id", fingerprint.id)
        object.__setattr__(self, "first_seen", first_seen)
        object.__setattr__(self, "last_seen", last_seen)
        object.__setattr__(self, "peak_rate_per_minute", int(peak_rate_per_minute))
        object.__setattr__(self, "occurrences", int(occurrences))
        object.__setattr__(self, "duration_seconds", dur)

    @property
    def firstSeen(self) -> datetime:
        return self.first_seen

    @property
    def lastSeen(self) -> datetime:
        return self.last_seen

    @property
    def peakRatePerMinute(self) -> int:
        return self.peak_rate_per_minute

    @property
    def duration(self) -> timedelta:
        return timedelta(seconds=self.duration_seconds)

    def get_fingerprint(self) -> Fingerprint:
        return self.fingerprint

    def getFingerprint(self) -> Fingerprint:
        return self.fingerprint

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_first_seen(self) -> datetime:
        return self.first_seen

    def getFirstSeen(self) -> datetime:
        return self.first_seen

    def get_last_seen(self) -> datetime:
        return self.last_seen

    def getLastSeen(self) -> datetime:
        return self.last_seen

    def get_peak_rate_per_minute(self) -> int:
        return self.peak_rate_per_minute

    def getPeakRatePerMinute(self) -> int:
        return self.peak_rate_per_minute

    def get_occurrences(self) -> int:
        return self.occurrences

    def getOccurrences(self) -> int:
        return self.occurrences

    def get_duration(self) -> timedelta:
        return self.duration

    def getDuration(self) -> timedelta:
        return self.duration

    def report(self) -> str:
        dur_str = (
            f"{self.duration_seconds // 60} min"
            if self.duration_seconds % 60 == 0
            else f"{self.duration_seconds} sec"
        )
        return (
            f"{self.id} burst detected\n\n"
            f"First Seen: {_format_utc_time(self.first_seen)}\n"
            f"Peak Rate: {self.peak_rate_per_minute}/min\n"
            f"Duration: {dur_str}"
        )


@dataclass(frozen=True)
class FailureAggregate:
    fingerprint: Fingerprint
    id: str
    occurrences: int

    def __init__(self, fingerprint: Fingerprint, occurrences: int) -> None:
        if fingerprint is None:
            raise ValueError("fingerprint must not be None")
        if occurrences < 1:
            raise ValueError("occurrences must be at least 1")
        object.__setattr__(self, "fingerprint", fingerprint)
        object.__setattr__(self, "id", fingerprint.id)
        object.__setattr__(self, "occurrences", int(occurrences))

    def get_fingerprint(self) -> Fingerprint:
        return self.fingerprint

    def getFingerprint(self) -> Fingerprint:
        return self.fingerprint

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_occurrences(self) -> int:
        return self.occurrences

    def getOccurrences(self) -> int:
        return self.occurrences


@dataclass(frozen=True)
class FailureFamilyAggregate:
    family: FailureFamily
    failures: tuple[FailureAggregate, ...]
    unique_failures: int
    occurrences: int

    def __init__(
        self,
        family: FailureFamily,
        failures: Iterable[FailureAggregate],
        occurrences: int,
    ) -> None:
        if family is None:
            raise ValueError("family must not be None")
        fails = tuple(failures)
        if not fails:
            raise ValueError("failures must not be empty")
        if occurrences < 1:
            raise ValueError("occurrences must be at least 1")
        object.__setattr__(self, "family", family)
        object.__setattr__(self, "failures", fails)
        object.__setattr__(self, "unique_failures", len(fails))
        object.__setattr__(self, "occurrences", int(occurrences))

    @property
    def uniqueFailures(self) -> int:
        return self.unique_failures

    def get_family(self) -> FailureFamily:
        return self.family

    def getFamily(self) -> FailureFamily:
        return self.family

    def get_failures(self) -> tuple[FailureAggregate, ...]:
        return self.failures

    def getFailures(self) -> tuple[FailureAggregate, ...]:
        return self.failures

    def get_unique_failures(self) -> int:
        return self.unique_failures

    def getUniqueFailures(self) -> int:
        return self.unique_failures

    def get_occurrences(self) -> int:
        return self.occurrences

    def getOccurrences(self) -> int:
        return self.occurrences


class _BurstAccumulator:
    def __init__(self, fingerprint: Fingerprint) -> None:
        self.fingerprint = fingerprint
        self.minute_counts: dict[int, int] = {}
        self.first_seen: Optional[datetime] = None
        self.last_seen: Optional[datetime] = None
        self.last_epoch_second: int = 0
        self.occurrences: int = 0
        self.peak_rate_per_minute: int = 0

    def add(self, occurrence: FailureOccurrence) -> None:
        dt = occurrence.occurred_at
        if self.first_seen is None or dt < self.first_seen:
            self.first_seen = dt
        if self.last_seen is None or dt > self.last_seen:
            self.last_seen = dt
            self.last_epoch_second = occurrence.epoch_second
        self.occurrences += 1
        minute_bucket = occurrence.epoch_second // 60
        count = self.minute_counts.get(minute_bucket, 0) + 1
        self.minute_counts[minute_bucket] = count
        if count > self.peak_rate_per_minute:
            self.peak_rate_per_minute = count

    def gap_before_seconds(self, epoch_second: int) -> int:
        return epoch_second - self.last_epoch_second

    def snapshot(self) -> FailureBurst:
        assert self.first_seen is not None and self.last_seen is not None
        return FailureBurst(
            fingerprint=self.fingerprint,
            first_seen=self.first_seen,
            last_seen=self.last_seen,
            peak_rate_per_minute=self.peak_rate_per_minute,
            occurrences=self.occurrences,
        )


class FailureTracker:
    """Thread-safe, in-memory aggregator for recurring failure fingerprints."""

    def __init__(self, timeline_limit: int = _DEFAULT_TIMELINE_LIMIT) -> None:
        if timeline_limit < 1:
            raise ValueError("timeline_limit must be at least 1")
        self._timeline_limit = timeline_limit
        self._lock = threading.RLock()
        self._failures: dict[str, tuple[Fingerprint, int]] = {}
        self._total_occurrences = 0
        self._timeline: deque[FailureOccurrence] = deque(maxlen=timeline_limit)

    def capture(
        self,
        failure: Union[BaseException, Fingerprint],
        occurred_at: Optional[Union[datetime, int, float]] = None,
    ) -> Fingerprint:
        if failure is None:
            raise ValueError("failure must not be None")
        if isinstance(failure, BaseException):
            fp = generate(failure)
            self.capture_fingerprint(fp, occurred_at)
            return fp
        self.capture_fingerprint(failure, occurred_at)
        return failure

    def capture_fingerprint(
        self,
        fingerprint: Fingerprint,
        occurred_at: Optional[Union[datetime, int, float]] = None,
    ) -> None:
        if fingerprint is None:
            raise ValueError("fingerprint must not be None")
        occ = FailureOccurrence(
            occurred_at if occurred_at is not None else datetime.now(timezone.utc),
            fingerprint,
        )
        with self._lock:
            existing = self._failures.get(fingerprint.id)
            if existing is not None:
                self._failures[fingerprint.id] = (existing[0], existing[1] + 1)
            else:
                self._failures[fingerprint.id] = (fingerprint, 1)
            self._total_occurrences += 1
            self._timeline.append(occ)

    def captureFingerprint(
        self,
        fingerprint: Fingerprint,
        occurred_at: Optional[Union[datetime, int, float]] = None,
    ) -> None:
        self.capture_fingerprint(fingerprint, occurred_at)

    def timeline(self) -> tuple[FailureOccurrence, ...]:
        with self._lock:
            snapshot = list(self._timeline)
        snapshot.sort(key=lambda o: (o.occurred_at, o.id))
        return tuple(snapshot)

    def timeline_report(self) -> str:
        return "\n".join(
            f"{_format_utc_time(occ.occurred_at)} {occ.id}"
            for occ in self.timeline()
        )

    def timelineReport(self) -> str:
        return self.timeline_report()

    def bursts(
        self,
        minimum_peak_rate_per_minute: int,
        maximum_idle_gap: Union[timedelta, int, float] = _DEFAULT_BURST_MAX_IDLE_GAP,
    ) -> tuple[FailureBurst, ...]:
        if minimum_peak_rate_per_minute < 1:
            raise ValueError("minimum_peak_rate_per_minute must be at least 1")
        max_gap_seconds = (
            int(maximum_idle_gap.total_seconds())
            if isinstance(maximum_idle_gap, timedelta)
            else int(maximum_idle_gap)
        )
        if max_gap_seconds <= 0:
            raise ValueError("maximum_idle_gap must be positive")

        accumulators: dict[str, _BurstAccumulator] = {}
        result: list[FailureBurst] = []

        for occ in self.timeline():
            acc = accumulators.get(occ.id)
            if (
                acc is not None
                and acc.gap_before_seconds(occ.epoch_second) > max_gap_seconds
            ):
                snap = acc.snapshot()
                if snap.peak_rate_per_minute >= minimum_peak_rate_per_minute:
                    result.append(snap)
                acc = None
            if acc is None:
                acc = _BurstAccumulator(occ.fingerprint)
                accumulators[occ.id] = acc
            acc.add(occ)

        for acc in accumulators.values():
            snap = acc.snapshot()
            if snap.peak_rate_per_minute >= minimum_peak_rate_per_minute:
                result.append(snap)

        result.sort(key=lambda b: (-b.peak_rate_per_minute, b.id))
        return tuple(result)

    def burst_report(self, minimum_peak_rate_per_minute: int) -> str:
        return "\n\n".join(
            b.report() for b in self.bursts(minimum_peak_rate_per_minute)
        )

    def burstReport(self, minimum_peak_rate_per_minute: int) -> str:
        return self.burst_report(minimum_peak_rate_per_minute)

    @property
    def timeline_limit(self) -> int:
        return self._timeline_limit

    def get_timeline_limit(self) -> int:
        return self._timeline_limit

    def getTimelineLimit(self) -> int:
        return self._timeline_limit

    def failures(self) -> tuple[FailureAggregate, ...]:
        with self._lock:
            snapshot = [
                FailureAggregate(fp, count) for fp, count in self._failures.values()
            ]
        snapshot.sort(key=lambda f: (-f.occurrences, f.id))
        return tuple(snapshot)

    def top_failures(
        self, limit: int = _DEFAULT_TOP_FAILURE_LIMIT
    ) -> tuple[FailureAggregate, ...]:
        if limit < 1:
            raise ValueError("limit must be at least 1")
        return self.failures()[:limit]

    def topFailures(
        self, limit: int = _DEFAULT_TOP_FAILURE_LIMIT
    ) -> tuple[FailureAggregate, ...]:
        return self.top_failures(limit)

    def families(self) -> tuple[FailureFamilyAggregate, ...]:
        grouped: dict[FailureFamily, list[FailureAggregate]] = {}
        counts: dict[FailureFamily, int] = {}

        for failure in self.failures():
            fam = failure.fingerprint.family
            grouped.setdefault(fam, []).append(failure)
            counts[fam] = counts.get(fam, 0) + failure.occurrences

        snapshot = [
            FailureFamilyAggregate(fam, list_f, counts[fam])
            for fam, list_f in grouped.items()
        ]
        snapshot.sort(key=lambda a: (-a.occurrences, a.family.value))
        return tuple(snapshot)

    def top_families(
        self, limit: int = _DEFAULT_TOP_FAMILY_LIMIT
    ) -> tuple[FailureFamilyAggregate, ...]:
        if limit < 1:
            raise ValueError("limit must be at least 1")
        return self.families()[:limit]

    def topFamilies(
        self, limit: int = _DEFAULT_TOP_FAMILY_LIMIT
    ) -> tuple[FailureFamilyAggregate, ...]:
        return self.top_families(limit)

    @property
    def total_occurrences(self) -> int:
        with self._lock:
            return self._total_occurrences

    def get_total_occurrences(self) -> int:
        return self.total_occurrences

    def getTotalOccurrences(self) -> int:
        return self.total_occurrences

    @property
    def unique_failures(self) -> int:
        with self._lock:
            return len(self._failures)

    def get_unique_failures(self) -> int:
        return self.unique_failures

    def getUniqueFailures(self) -> int:
        return self.unique_failures

    @property
    def unique_families(self) -> int:
        return len(self.families())

    def get_unique_families(self) -> int:
        return self.unique_families

    def getUniqueFamilies(self) -> int:
        return self.unique_families

    def report(self) -> str:
        grouped = self.failures()
        plural = "" if len(grouped) == 1 else "s"
        out = f"{len(grouped)} unique failure signature{plural}"
        for failure in grouped:
            out += f"\n\n{failure.id}\nCount: {failure.occurrences}"
        return out

    def top_failure_report(self, limit: int = _DEFAULT_TOP_FAILURE_LIMIT) -> str:
        out = f"Top {limit} Failure Signatures"
        for failure in self.top_failures(limit):
            out += f"\n{failure.id}\nCount: {failure.occurrences}"
        return out

    def topFailureReport(self, limit: int = _DEFAULT_TOP_FAILURE_LIMIT) -> str:
        return self.top_failure_report(limit)

    def family_report(self) -> str:
        return self._format_family_report("Root Cause Families", self.families())

    def familyReport(self) -> str:
        return self.family_report()

    def top_family_report(self, limit: int = _DEFAULT_TOP_FAMILY_LIMIT) -> str:
        if limit < 1:
            raise ValueError("limit must be at least 1")
        return self._format_family_report(
            f"Top {limit} Root Cause Families", self.top_families(limit)
        )

    def topFamilyReport(self, limit: int = _DEFAULT_TOP_FAMILY_LIMIT) -> str:
        return self.top_family_report(limit)

    def clear(self) -> None:
        with self._lock:
            self._failures.clear()
            self._total_occurrences = 0
            self._timeline.clear()

    @staticmethod
    def _format_family_report(
        heading: str, families: Iterable[FailureFamilyAggregate]
    ) -> str:
        out = heading
        for fam in families:
            out += (
                f"\n\nFamily: {fam.family.value}\n"
                f"Occurrences: {fam.occurrences}\n"
                f"Unique Failures: {fam.unique_failures}"
            )
            for failure in fam.failures:
                out += f"\n{failure.id} ({failure.occurrences})"
        return out


class SkipReasonAnalyzer:
    def __init__(self, tracker: Optional[FailureTracker] = None) -> None:
        self._tracker = tracker if tracker is not None else FailureTracker()

    def record(self, failure: Union[BaseException, Fingerprint]) -> Fingerprint:
        if failure is None:
            raise ValueError("failure must not be None")
        return self._tracker.capture(failure)

    @property
    def most_common_failure(self) -> Optional[FailureAggregate]:
        top = self._tracker.top_failures(1)
        return top[0] if top else None

    def get_most_common_failure(self) -> Optional[FailureAggregate]:
        return self.most_common_failure

    def getMostCommonFailure(self) -> Optional[FailureAggregate]:
        return self.most_common_failure

    def report(self) -> str:
        common = self.most_common_failure
        if common is None:
            return "No skipped records captured"
        return (
            f"Most common skip reason: {common.id} ({common.occurrences})\n\n"
            f"{self._tracker.top_failure_report()}"
        )

    def clear(self) -> None:
        self._tracker.clear()


@dataclass(frozen=True)
class ConsumerFailureAggregate:
    topic: str
    partition: int
    offset: int
    fingerprint: Fingerprint
    id: str
    occurrences: int

    def __init__(
        self,
        topic: str,
        partition: int,
        offset: int,
        fingerprint: Fingerprint,
        occurrences: int,
    ) -> None:
        object.__setattr__(self, "topic", topic)
        object.__setattr__(self, "partition", int(partition))
        object.__setattr__(self, "offset", int(offset))
        object.__setattr__(self, "fingerprint", fingerprint)
        object.__setattr__(self, "id", fingerprint.id)
        object.__setattr__(self, "occurrences", int(occurrences))

    def get_topic(self) -> str:
        return self.topic

    def getTopic(self) -> str:
        return self.topic

    def get_partition(self) -> int:
        return self.partition

    def getPartition(self) -> int:
        return self.partition

    def get_offset(self) -> int:
        return self.offset

    def getOffset(self) -> int:
        return self.offset

    def get_fingerprint(self) -> Fingerprint:
        return self.fingerprint

    def getFingerprint(self) -> Fingerprint:
        return self.fingerprint

    def get_id(self) -> str:
        return self.id

    def getId(self) -> str:
        return self.id

    def get_occurrences(self) -> int:
        return self.occurrences

    def getOccurrences(self) -> int:
        return self.occurrences


class ConsumerFailureTracker:
    def __init__(self) -> None:
        self._lock = threading.RLock()
        self._entries: dict[tuple[str, str], ConsumerFailureAggregate] = {}

    def capture(
        self,
        topic: str,
        partition: int,
        offset: int,
        failure: Union[BaseException, Fingerprint],
    ) -> Fingerprint:
        if not topic or not topic.strip():
            raise ValueError("topic must not be blank")
        if partition < 0:
            raise ValueError("partition must not be negative")
        if offset < 0:
            raise ValueError("offset must not be negative")
        if failure is None:
            raise ValueError("failure must not be None")
        fp = generate(failure) if isinstance(failure, BaseException) else failure
        key = (topic, fp.id)
        with self._lock:
            existing = self._entries.get(key)
            count = (existing.occurrences + 1) if existing is not None else 1
            self._entries[key] = ConsumerFailureAggregate(
                topic=topic,
                partition=partition,
                offset=offset,
                fingerprint=fp,
                occurrences=count,
            )
        return fp

    def failures(self) -> tuple[ConsumerFailureAggregate, ...]:
        with self._lock:
            result = list(self._entries.values())
        result.sort(key=lambda e: (-e.occurrences, e.id, e.topic))
        return tuple(result)

    def report(self) -> str:
        items = self.failures()
        if not items:
            return "No consumer failures captured"
        return "\n\n".join(
            f"Topic: {item.topic}\n"
            f"Partition: {item.partition}\n"
            f"Offset: {item.offset}\n"
            f"Fingerprint: {item.id}\n"
            f"Occurrences: {item.occurrences}"
            for item in items
        )

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()
