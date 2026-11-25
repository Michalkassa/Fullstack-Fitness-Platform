import { caloriesFromMacros, percentOf } from "./nutrition";

describe("caloriesFromMacros", () => {
  it("uses 4/4/9 kcal per gram", () => {
    expect(caloriesFromMacros(30, 40, 10)).toBe(370);
  });
  it("is zero for no macros", () => {
    expect(caloriesFromMacros(0, 0, 0)).toBe(0);
  });
  it("rounds to the nearest kcal", () => {
    expect(caloriesFromMacros(10.4, 0, 0)).toBe(42);
  });
  it("treats non-finite inputs as zero", () => {
    expect(caloriesFromMacros(NaN, 20, 5)).toBe(125);
  });
});

describe("percentOf", () => {
  it("clamps to 100 and handles a zero goal", () => {
    expect(percentOf(50, 200)).toBe(25);
    expect(percentOf(300, 200)).toBe(100);
    expect(percentOf(10, 0)).toBe(0);
  });
});
