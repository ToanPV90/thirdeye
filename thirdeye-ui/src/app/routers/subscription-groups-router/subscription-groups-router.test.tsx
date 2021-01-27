import { render, screen } from "@testing-library/react";
import React from "react";
import { MemoryRouter } from "react-router-dom";
import { Breadcrumb } from "../../components/breadcrumbs/breadcrumbs.interfaces";
import { PageContainer } from "../../components/page-container/page-container.component";
import { AppRoute } from "../../utils/routes-util/routes-util";
import { SubscriptionGroupsRouter } from "./subscription-groups-router";

jest.mock("../../components/app-breadcrumbs/app-breadcrumbs.component", () => ({
    useAppBreadcrumbs: jest.fn().mockImplementation(() => ({
        setRouterBreadcrumbs: mockSetRouterBreadcrumbs,
    })),
}));

jest.mock("react-router-dom", () => ({
    ...(jest.requireActual("react-router-dom") as Record<string, unknown>),
    useHistory: jest.fn().mockImplementation(() => ({
        push: mockPush,
    })),
}));

jest.mock("react-i18next", () => ({
    useTranslation: jest.fn().mockReturnValue({
        t: (key: string): string => {
            return key;
        },
    }),
}));

jest.mock("../../utils/routes-util/routes-util", () => ({
    ...(jest.requireActual("../../utils/routes-util/routes-util") as Record<
        string,
        unknown
    >),
    getConfigurationPath: jest.fn().mockReturnValue("testConfigurationPath"),
    getSubscriptionGroupsPath: jest
        .fn()
        .mockReturnValue("testSubscriptionGroupsPath"),
}));

jest.mock("../../components/page-container/page-container.component", () => ({
    PageContainer: jest.fn().mockReturnValue(<>testPageContainer</>),
}));

jest.mock(
    "../../pages/subscription-groups-all-page/subscription-groups-all-page.component",
    () => ({
        SubscriptionGroupsAllPage: jest
            .fn()
            .mockReturnValue(<>testSubscriptionGroupsAllPage</>),
    })
);

jest.mock(
    "../../pages/subscription-groups-detail-page/subscription-groups-detail-page.component",
    () => ({
        SubscriptionGroupsDetailPage: jest
            .fn()
            .mockReturnValue(<>testSubscriptionGroupsDetailPage</>),
    })
);

jest.mock(
    "../../pages/subscription-groups-create-page/subscription-groups-create-page.component",
    () => ({
        SubscriptionGroupsCreatePage: jest
            .fn()
            .mockReturnValue(<>testSubscriptionGroupsCreatePage</>),
    })
);

jest.mock(
    "../../pages/subscription-groups-update-page/subscription-groups-update-page.component",
    () => ({
        SubscriptionGroupsUpdatePage: jest
            .fn()
            .mockReturnValue(<>testSubscriptionGroupsUpdatePage</>),
    })
);

jest.mock(
    "../../pages/page-not-found-page/page-not-found-page.component",
    () => ({
        PageNotFoundPage: jest.fn().mockReturnValue(<>testPageNotFoundPage</>),
    })
);

describe("Subscription Groups Router", () => {
    test("should have rendered page container while loading", () => {
        render(
            <MemoryRouter>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(PageContainer).toHaveBeenCalled();
    });

    test("should set appropriate router breadcrumbs", () => {
        render(
            <MemoryRouter>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        // Get router breadcrumbs
        const breadcrumbs: Breadcrumb[] =
            mockSetRouterBreadcrumbs.mock.calls[0][0];
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
        expect(breadcrumbs[1].text).toEqual("label.subscription-groups");
        expect(breadcrumbs[1].onClick).toBeDefined();
        expect(mockPush).toHaveBeenNthCalledWith(
            2,
            "testSubscriptionGroupsPath"
        );
    });

    test("should render subscription groups all page at exact subscription groups path", async () => {
        render(
            <MemoryRouter initialEntries={[AppRoute.SUBSCRIPTION_GROUPS]}>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testSubscriptionGroupsAllPage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at invalid subscription groups path", async () => {
        render(
            <MemoryRouter
                initialEntries={[`${AppRoute.SUBSCRIPTION_GROUPS}/testPath`]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render subscription groups all page at exact subscription groups all path", async () => {
        render(
            <MemoryRouter initialEntries={[AppRoute.SUBSCRIPTION_GROUPS_ALL]}>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testSubscriptionGroupsAllPage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at invalid subscription groups all path", async () => {
        render(
            <MemoryRouter
                initialEntries={[
                    `${AppRoute.SUBSCRIPTION_GROUPS_ALL}/testPath`,
                ]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render subscription groups detail page at exact subscription groups detail path", async () => {
        render(
            <MemoryRouter
                initialEntries={[AppRoute.SUBSCRIPTION_GROUPS_DETAIL]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testSubscriptionGroupsDetailPage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at invalid subscription groups detail path", async () => {
        render(
            <MemoryRouter
                initialEntries={[
                    `${AppRoute.SUBSCRIPTION_GROUPS_DETAIL}/testPath`,
                ]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render subscription groups create page at exact subscription groups create path", async () => {
        render(
            <MemoryRouter
                initialEntries={[AppRoute.SUBSCRIPTION_GROUPS_CREATE]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testSubscriptionGroupsCreatePage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at invalid subscription groups create path", async () => {
        render(
            <MemoryRouter
                initialEntries={[
                    `${AppRoute.SUBSCRIPTION_GROUPS_CREATE}/testPath`,
                ]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render subscription groups update page at exact subscription groups update path", async () => {
        render(
            <MemoryRouter
                initialEntries={[AppRoute.SUBSCRIPTION_GROUPS_UPDATE]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testSubscriptionGroupsUpdatePage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at invalid subscription groups update path", async () => {
        render(
            <MemoryRouter
                initialEntries={[
                    `${AppRoute.SUBSCRIPTION_GROUPS_UPDATE}/testPath`,
                ]}
            >
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render page not found page at any other path", async () => {
        render(
            <MemoryRouter initialEntries={["/testPath"]}>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });

    test("should render page not found page by default", async () => {
        render(
            <MemoryRouter>
                <SubscriptionGroupsRouter />
            </MemoryRouter>
        );

        expect(
            await screen.findByText("testPageNotFoundPage")
        ).toBeInTheDocument();
    });
});

const mockSetRouterBreadcrumbs = jest.fn();

const mockPush = jest.fn();
