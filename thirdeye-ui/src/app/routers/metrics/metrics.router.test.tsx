import { render, screen } from "@testing-library/react";
import React from "react";
import { MemoryRouter } from "react-router-dom";
import { LoadingIndicator } from "../../components/loading-indicator/loading-indicator.component";
import { AppRoute } from "../../utils/routes/routes.util";
import { MetricsRouter } from "./metrics.router";

jest.mock(
    "../../components/app-breadcrumbs/app-breadcrumbs-provider/app-breadcrumbs-provider.component",
    () => ({
        useAppBreadcrumbs: jest.fn().mockImplementation(() => ({
            setRouterBreadcrumbs: mockSetRouterBreadcrumbs,
        })),
    })
);

jest.mock("react-router-dom", () => ({
    ...(jest.requireActual("react-router-dom") as Record<string, unknown>),
    useHistory: jest.fn().mockImplementation(() => ({
        push: mockPush,
    })),
}));

jest.mock("react-i18next", () => ({
    useTranslation: jest.fn().mockReturnValue({
        t: (key: string) => key,
    }),
}));

jest.mock("../../utils/routes/routes.util", () => ({
    ...(jest.requireActual("../../utils/routes/routes.util") as Record<
        string,
        unknown
    >),
    getConfigurationPath: jest.fn().mockReturnValue("testConfigurationPath"),
    getMetricsPath: jest.fn().mockReturnValue("testMetricsPath"),
}));

jest.mock(
    "../../components/loading-indicator/loading-indicator.component",
    () => ({
        LoadingIndicator: jest.fn().mockReturnValue("testLoadingIndicator"),
    })
);

jest.mock("../../pages/metrics-all-page/metrics-all-page.component", () => ({
    MetricsAllPage: jest.fn().mockReturnValue("testMetricsAllPage"),
}));

jest.mock("../../pages/metrics-view-page/metrics-view-page.component", () => ({
    MetricsViewPage: jest.fn().mockReturnValue("testMetricsViewPage"),
}));

jest.mock(
    "../../pages/page-not-found-page/page-not-found-page.component",
    () => ({
        PageNotFoundPage: jest.fn().mockReturnValue("testPageNotFoundPage"),
    })
);

describe("Metrics Router", () => {
    test("should have rendered loading indicator while loading", () => {
        render(
            <MemoryRouter>
                <MetricsRouter />
            </MemoryRouter>
        );

        expect(LoadingIndicator).toHaveBeenCalled();
    });

    test("should set appropriate router breadcrumbs", () => {
        render(
            <MemoryRouter>
                <MetricsRouter />
            </MemoryRouter>
        );

        expect(mockSetRouterBreadcrumbs).toHaveBeenCalled();

        // Get router breadcrumbs
        const breadcrumbs = mockSetRouterBreadcrumbs.mock.calls[0][0];
        // Also invoke the click handlers
        breadcrumbs &&
            breadcrumbs[0] &&
            breadcrumbs[0].onClick &&
            breadcrumbs[0].onClick();
        breadcrumbs &&
            breadcrumbs[1] &&
            breadcrumbs[1].onClick &&
            breadcrumbs[1].onClick();

        expect(breadcrumbs).toHaveLength(2);
        expect(breadcrumbs[0].text).toEqual("label.configuration");
        expect(breadcrumbs[0].onClick).toBeDefined();
        expect(mockPush).toHaveBeenNthCalledWith(1, "testConfigurationPath");
        expect(breadcrumbs[1].text).toEqual("label.metrics");
        expect(breadcrumbs[1].onClick).toBeDefined();
        expect(mockPush).toHaveBeenNthCalledWith(2, "testMetricsPath");
    });

    test("should render metrics all page at exact metrics path", async () => {
        render(
            <MemoryRouter initialEntries={[AppRoute.METRICS]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testMetricsAllPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render page not found page at invalid metrics path", async () => {
        render(
            <MemoryRouter initialEntries={[`${AppRoute.METRICS}/testPath`]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testPageNotFoundPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render metrics all page at exact metrics all path", async () => {
        render(
            <MemoryRouter initialEntries={[AppRoute.METRICS_ALL]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testMetricsAllPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render page not found page at invalid metrics all path", async () => {
        render(
            <MemoryRouter initialEntries={[`${AppRoute.METRICS_ALL}/testPath`]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testPageNotFoundPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render metrics view page at exact metrics view path", async () => {
        render(
            <MemoryRouter initialEntries={[AppRoute.METRICS_VIEW]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testMetricsViewPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render page not found page at invalid metrics view path", async () => {
        render(
            <MemoryRouter
                initialEntries={[`${AppRoute.METRICS_VIEW}/testPath`]}
            >
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testPageNotFoundPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render page not found page at any other path", async () => {
        render(
            <MemoryRouter initialEntries={["/testPath"]}>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testPageNotFoundPage")
        ).resolves.toBeInTheDocument();
    });

    test("should render page not found page by default", async () => {
        render(
            <MemoryRouter>
                <MetricsRouter />
            </MemoryRouter>
        );

        await expect(
            screen.findByText("testPageNotFoundPage")
        ).resolves.toBeInTheDocument();
    });
});

const mockSetRouterBreadcrumbs = jest.fn();

const mockPush = jest.fn();
