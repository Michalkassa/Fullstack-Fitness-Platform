import { MAX_REPS, validateLogin, validateMacros, validateNamed, validateRegister, validateSet } from "./validation";

describe("validation", () => {
  it("checks login input", () => {
    expect(validateLogin({ email: "", password: "x" })).toBe("Please enter an email");
    expect(validateLogin({ email: "nope", password: "x" })).toBe("Not a valid email");
    expect(validateLogin({ email: "a@b.com", password: "x" })).toBeNull();
  });
  it("checks registration input", () => {
    expect(validateRegister({ email: "a@b.com", password: "short", confirmPassword: "short" })).toMatch(/at least/);
    expect(validateRegister({ email: "a@b.com", password: "longenough", confirmPassword: "different" })).toBe(
      "Passwords do not match",
    );
    expect(validateRegister({ email: "a@b.com", password: "longenough", confirmPassword: "longenough" })).toBeNull();
  });
  it("checks names", () => {
    expect(validateNamed({ name: "Bench", description: "" })).toBe("Enter a name and a description");
    expect(validateNamed({ name: "a".repeat(41), description: "d" })).toMatch(/at most/);
  });
  it("checks sets", () => {
    expect(validateSet({ weight: "100", reps: "5" })).toBeNull();
    expect(validateSet({ weight: "100", reps: "5.5" })).not.toBeNull();
    expect(validateSet({ weight: "", reps: "5" })).not.toBeNull();
  });
  it("caps reps at 36, matching the backend", () => {
    expect(MAX_REPS).toBe(36);
    expect(validateSet({ weight: "100", reps: "36" })).toBeNull();
    expect(validateSet({ weight: "100", reps: "37" })).toBe("At most 36 reps per set");
    expect(validateSet({ weight: "100", reps: "0" })).toBe("Enter a weight and the reps");
  });
  it("checks macros", () => {
    expect(validateMacros({ calories: "", protein: "", carbs: "", fat: "" })).toBe("Enter a calorie amount");
    expect(validateMacros({ calories: "500", protein: "-1", carbs: "", fat: "" })).toBe("Macros cannot be negative");
    expect(validateMacros({ calories: "500", protein: "", carbs: "", fat: "" })).toBeNull();
  });
});
