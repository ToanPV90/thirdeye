import React, { FunctionComponent } from "react";
import { Route, Switch } from "react-router-dom";
import { useAuth } from "../../components/auth-provider/auth-provider.component";
import { AppRoute } from "../../utils/routes-util/routes-util";
import { AlertsRouter } from "../alerts-router/alerts-router";
import { AnomaliesRouter } from "../anomalies-router/anomalies-router";
import { ConfigurationRouter } from "../configuration-router/configuration-router";
import { GeneralAuthenticatedRouter } from "../general-authenticated-router/general-authenticated-router";
import { GeneralUnauthenticatedRouter } from "../general-unauthenticated-router/general-unauthenticated-router";

export const AppRouter: FunctionComponent = () => {
    const { authDisabled, authenticated } = useAuth();

    if (authDisabled || authenticated) {
        return (
            <Switch>
                {/* Direct all alerts paths to alerts router */}
                <Route component={AlertsRouter} path={AppRoute.ALERTS} />

                {/* Direct all anomalies paths to anomalies router */}
                <Route component={AnomaliesRouter} path={AppRoute.ANOMALIES} />

                {/* Direct all configuration paths to configuration router */}
                <Route
                    component={ConfigurationRouter}
                    path={AppRoute.CONFIGURATION}
                />

                {/* Direct all other paths to general authenticated router */}
                <Route component={GeneralAuthenticatedRouter} />
            </Switch>
        );
    }

    return (
        // Not authenticated, direct all paths to general unauthenticated router
        <GeneralUnauthenticatedRouter />
    );
};
