/**
 * Copyright 2022 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
import { Box, Divider, Grid, Link, Typography } from "@material-ui/core";
import InfoIcon from "@material-ui/icons/Info";
import Alert from "@material-ui/lab/Alert";
import React, { FunctionComponent } from "react";
import { useTranslation } from "react-i18next";
import { JSONEditorV1 } from "../../../platform/components";
import { EditableAlert } from "../../../rest/dto/alert.interfaces";
import { getAlertTemplatesAllPath } from "../../../utils/routes/routes.util";
import { useAlertWizardV2Styles } from "../alert-wizard-v2.styles";
import { AlertJsonProps } from "./alert-json.interfaces";

export const AlertJson: FunctionComponent<AlertJsonProps> = ({
    alert,
    onAlertPropertyChange,
}) => {
    const { t } = useTranslation();
    const classes = useAlertWizardV2Styles();

    const handleJSONChange = (json: string): void => {
        onAlertPropertyChange(JSON.parse(json), true);
    };

    return (
        <Grid container item xs={12}>
            <Grid item xs={12}>
                <Box marginBottom={2}>
                    <Typography variant="h5">
                        {t("label.advanced-template-configuration-json-editor")}
                    </Typography>
                    <Typography variant="body2">
                        {t(
                            "message.attributes-different-from-simple-view-may-not-reflect"
                        )}
                    </Typography>
                </Box>
                <Alert
                    className={classes.infoAlert}
                    icon={<InfoIcon />}
                    severity="info"
                >
                    {t("message.changes-added-to-template-properties")}
                    <Link href={getAlertTemplatesAllPath()} target="_blank">
                        {t("label.template-configuration").toLowerCase()}
                    </Link>
                </Alert>
                <Box paddingBottom={2} paddingTop={2}>
                    <Divider />
                </Box>
            </Grid>

            <Grid item xs={12}>
                <JSONEditorV1<EditableAlert>
                    hideValidationSuccessIcon
                    value={alert}
                    onChange={handleJSONChange}
                />
            </Grid>
        </Grid>
    );
};