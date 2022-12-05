/*
 * Copyright 2022 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
import {
    Box,
    Card,
    CardContent,
    CardProps,
    Typography,
} from "@material-ui/core";
import React, { FunctionComponent } from "react";
import { AlgorithmOptionCardProps } from "./algorithm-option-card.interfaces";

const OutlineCardComponent: FunctionComponent<CardProps> = (props) => {
    return <Card variant="outlined" {...props} />;
};

export const AlgorithmOptionCard: FunctionComponent<AlgorithmOptionCardProps> =
    ({ option, children }) => {
        return (
            <Box
                component={OutlineCardComponent}
                display="flex"
                flexDirection="column"
                height="100%"
                justifyContent="space-between"
            >
                <Box>
                    <CardContent>
                        <Typography gutterBottom variant="subtitle1">
                            {option.title}
                        </Typography>
                        <Typography component="p" variant="body2">
                            {option.description}
                        </Typography>
                    </CardContent>
                </Box>
                <Box>{children}</Box>
            </Box>
        );
    };