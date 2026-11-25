import { dayKey, daysAgo, startOfLocalDay } from "./dates";

describe("dates", () => {
  it("formats a UTC day key", () => {
    expect(dayKey("2026-06-11T13:45:00Z")).toBe("2026-06-11");
  });
  it("counts whole days ago", () => {
    const now = new Date("2026-06-11T12:00:00Z");
    expect(daysAgo("2026-06-04T12:00:00Z", now)).toBe(7);
    expect(daysAgo("2026-06-11T01:00:00Z", now)).toBe(0);
  });
  it("finds local midnight", () => {
    const d = startOfLocalDay(new Date(2026, 5, 11, 15, 30));
    expect([d.getHours(), d.getMinutes(), d.getDate()]).toEqual([0, 0, 11]);
  });
});
