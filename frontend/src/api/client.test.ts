import { ApiError, http, setToken } from "./client";

function mockFetch(status: number, body?: unknown) {
  const fn = vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fn);
  return fn;
}

describe("api client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    setToken(null);
  });

  it("sends the bearer token and parses JSON", async () => {
    setToken("abc");
    const fetch = mockFetch(200, { ok: true });
    await expect(http.get("/api/x")).resolves.toEqual({ ok: true });
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe("Bearer abc");
  });

  it("returns undefined for 204", async () => {
    mockFetch(204);
    await expect(http.delete("/api/x/1")).resolves.toBeUndefined();
  });

  it("turns problem details into ApiError", async () => {
    mockFetch(400, { title: "Bad Request", detail: "Weight must be positive", errors: { weight: "must be positive" } });
    const err = await http.post("/api/x", {}).catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.message).toBe("Weight must be positive");
    expect(err.fieldErrors).toEqual({ weight: "must be positive" });
  });
});
