import { changeOver, percentChange, weightNDaysAgo, weightStats } from "./bodyweight";

const now = new Date("2026-06-30T12:00:00Z");
const points = [
  { weight: 90, createdAt: "2026-05-31T12:00:00Z" }, // 30 days ago
  { weight: 85, createdAt: "2026-06-23T12:00:00Z" }, // 7 days ago
  { weight: 80, createdAt: "2026-06-30T10:00:00Z" }, // today
];

describe("weightStats", () => {
  it("computes highest, lowest and average", () => {
    expect(weightStats(points)).toEqual({ highest: 90, lowest: 80, average: 85 });
  });
  it("is all zeros for no data", () => {
    expect(weightStats([])).toEqual({ highest: 0, lowest: 0, average: 0 });
  });
});

describe("weight change", () => {
  it("picks the entry closest to N days ago", () => {
    expect(weightNDaysAgo(points, 7, now)).toBe(85);
    expect(weightNDaysAgo(points, 25, now)).toBe(90);
  });
  it("computes the percentage change to 2 d.p.", () => {
    expect(percentChange(80, 90)).toBe(-11.11);
    expect(changeOver(points, 7, now)).toBe(-5.88);
    expect(changeOver([], 7, now)).toBe(0);
  });
});
