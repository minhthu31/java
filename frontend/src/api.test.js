describe("API Base URL Configuration", () => {
    const originalEnv = process.env;

    beforeEach(() => {
        jest.resetModules();
        process.env = { ...originalEnv };
    });

    afterAll(() => {
        process.env = originalEnv;
    });

    test("should fallback to default URL when env is not set", () => {
        delete process.env.REACT_APP_API_BASE_URL;
        delete process.env.REACT_APP_API_URL;
        const api = require("./api").default;
        expect(api.defaults.baseURL).toBe("http://localhost:8080/api/v1");
    });

    test("should use custom REACT_APP_API_BASE_URL when provided", () => {
        process.env.REACT_APP_API_BASE_URL = "http://localhost:9090/api/v1";
        const api = require("./api").default;
        expect(api.defaults.baseURL).toBe("http://localhost:9090/api/v1");
    });
});
