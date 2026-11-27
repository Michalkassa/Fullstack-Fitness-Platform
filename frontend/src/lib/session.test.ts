import { exceedsRepCap, formatElapsed, loadDraft, saveDraft, toSessionRequest } from "./session";

describe("toSessionRequest", () => {
  it("keeps only complete, valid sets and drops empty exercises", () => {
    const req = toSessionRequest(
      {
        startedAt: 0,
        sets: {
          a: [
            { weight: "100", reps: "5" },
            { weight: "", reps: "5" },
            { weight: "100", reps: "2.5" },
          ],
          b: [{ weight: "", reps: "" }],
          c: [{ weight: "0", reps: "10" }],
        },
      },
      ["a", "b", "c"],
    );
    expect(req).toEqual({
      exercises: [
        { exerciseId: "a", sets: [{ weight: 100, reps: 5 }] },
        { exerciseId: "c", sets: [{ weight: 0, reps: 10 }] },
      ],
    });
  });
});

describe("rep cap", () => {
  it("never submits a set above 36 reps", () => {
    const req = toSessionRequest(
      { startedAt: 0, sets: { a: [{ weight: "50", reps: "36" }, { weight: "50", reps: "37" }] } },
      ["a"],
    );
    expect(req).toEqual({ exercises: [{ exerciseId: "a", sets: [{ weight: 50, reps: 36 }] }] });
  });
  it("flags any set above 36 reps so it isn't dropped silently", () => {
    expect(exceedsRepCap({ a: [{ weight: "50", reps: "36" }], b: [{ weight: "", reps: "" }] })).toBe(false);
    expect(exceedsRepCap({ a: [{ weight: "50", reps: "5" }], b: [{ weight: "20", reps: "40" }] })).toBe(true);
  });
});

describe("draft persistence", () => {
  beforeEach(() => localStorage.clear());

  it("round-trips through localStorage", () => {
    saveDraft("w1", { startedAt: 123, sets: { a: [{ weight: "1", reps: "2" }] } });
    expect(loadDraft("w1").sets.a).toEqual([{ weight: "1", reps: "2" }]);
  });
  it("starts fresh on corrupted data", () => {
    localStorage.setItem("lockedin.session.w1", "{nope");
    expect(loadDraft("w1", 42)).toEqual({ startedAt: 42, sets: {} });
  });
});

describe("formatElapsed", () => {
  it("formats hh:mm:ss", () => {
    expect(formatElapsed(3_723_000)).toBe("01:02:03");
    expect(formatElapsed(-5)).toBe("00:00:00");
  });
});
