import React, { FunctionComponent, useEffect, useState } from "react";
import { Redirect, Route, Switch, useLocation } from "react-router-dom";
import { Breadcrumb } from "../../components/application-breadcrumbs/application-breadcrumbs.interfaces";
import { PageContainer } from "../../components/page-container/page-container.component";
import { PageLoadingIndicator } from "../../components/page-loading-indicator/page-loading-indicator.component";
import { SignInPage } from "../../pages/sign-in/sign-in-page.component";
import { useApplicationBreadcrumbsStore } from "../../store/application-breadcrumbs/application-breadcrumbs-store";
import { useRedirectionPathStore } from "../../store/redirection-path/redirection-path-store";
import {
    ApplicationRoute,
    getSignInPath,
} from "../../utils/routes-util/routes-util";

export const GeneralUnauthenticatedRouter: FunctionComponent = () => {
    const [loading, setLoading] = useState(true);
    const [
        setAppSectionBreadcrumb,
    ] = useApplicationBreadcrumbsStore((state) => [
        state.setAppSectionBreadcrumb,
    ]);
    const [setRedirectToPath] = useRedirectionPathStore((state) => [
        state.setRedirectToPath,
    ]);
    const location = useLocation();

    useEffect(() => {
        // Create app section breadcrumb
        setAppSectionBreadcrumb({} as Breadcrumb);

        setRedirectionPath();

        setLoading(false);
    }, []);

    const setRedirectionPath = (): void => {
        // If location is anything other than the sign in/out path, store it to redirect the user
        // after potential authentication
        if (
            location.pathname === ApplicationRoute.SIGN_IN ||
            location.pathname === ApplicationRoute.SIGN_OUT
        ) {
            return;
        }

        setRedirectToPath(location.pathname);
    };

    if (loading) {
        return (
            <PageContainer>
                <PageLoadingIndicator />
            </PageContainer>
        );
    }

    return (
        <Switch>
            {/* Sign in path */}
            <Route
                exact
                component={SignInPage}
                path={ApplicationRoute.SIGN_IN}
            />

            {/* No match found, redirect to sign in path */}
            <Redirect to={getSignInPath()} />
        </Switch>
    );
};
