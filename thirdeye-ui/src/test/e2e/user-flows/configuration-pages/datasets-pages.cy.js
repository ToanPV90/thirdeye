/*
 * Copyright 2023 StarTree Inc
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

import { DATASET_FORM_TEST_IDS } from "../../../../app/components/dataset-create-wizard/dataset-properties-form/dataset-properties-form.interfaces";
import { TEST_IDS } from "../../../../app/components/dataset-list-v1/dataset-list-v1.interfaces";

const CHECKBOX_SELECTOR = ".MuiDataGrid-cell input[type='checkbox']";

describe("configuration datasets pages", () => {
    beforeEach(() => {
        // Clear out any existing data sources
        cy.resetDatasets();
        cy.loadDatasource();
        cy.visit("http://localhost:7004/configuration/datasets");
    });

    it("user can edit dataset", () => {
        cy.get(".MuiDataGrid-main").find(CHECKBOX_SELECTOR).click();

        cy.getByDataTestId(TEST_IDS.EDIT_BUTTON).click();

        // Should match something like
        // `http://localhost:7004/configuration/dataset/update/id/30521`
        cy.url().should("contain", "/configuration/datasets/update/id/");
    });

    it("user can delete dataset", () => {
        cy.get(".MuiDataGrid-main").find(CHECKBOX_SELECTOR).click();

        cy.getByDataTestId(TEST_IDS.DELETE_BUTTON).click();

        cy.intercept("DELETE", "/api/datasets/*").as("deleteDatasetRequest");
        cy.intercept("DELETE", "/api/metrics/*").as("deleteMetricRequest");

        // Click confirm button in the dialog
        cy.get("[role='dialog']").contains("Confirm").click();

        // Ensure datasets and metrics are also requested to be deleted
        cy.wait("@deleteDatasetRequest")
            .its("response.statusCode")
            .should("eq", 200);
        cy.wait("@deleteMetricRequest")
            .its("response.statusCode")
            .should("eq", 200);

        // Since there is only one datasource, the table should no longer exist after deleting
        cy.get(".MuiDataGrid-main").should("not.exist");
    });

    it("user can create multiple datasets", () => {
        cy.getByDataTestId("create-menu-button").click();

        cy.get("ul.MuiMenu-list")
            .find("li[role='menuitem']")
            .contains("Onboard Dataset")
            .click();

        cy.getByDataTestId(
            DATASET_FORM_TEST_IDS.DATASOURCE_AUTOCOMPLETE_TEXT_BOX
        ).click();

        cy.get("ul.MuiAutocomplete-listbox")
            .find("li")
            .contains("mypinot")
            .click();

        cy.get("input[type='checkbox']").click({ multiple: true });

        cy.intercept("GET", "/api/datasets").as("getDatasets");

        cy.get("button").contains("Submit").click();
        cy.get(".notification-display-v1")
            .contains("Datasets onboarded successfully")
            .should("be.visible");

        cy.wait("@getDatasets").its("response.body.length").should("eq", 4);
    });

    it("user can create one dataset", () => {
        cy.getByDataTestId("create-menu-button").click();

        cy.get("ul.MuiMenu-list")
            .find("li[role='menuitem']")
            .contains("Onboard Dataset")
            .click();

        cy.getByDataTestId(
            DATASET_FORM_TEST_IDS.DATASOURCE_AUTOCOMPLETE_TEXT_BOX
        ).click();

        cy.get("ul.MuiAutocomplete-listbox")
            .find("li")
            .contains("mypinot")
            .click();

        cy.get("input[type='checkbox']").first().click();

        cy.get("button").contains("Submit").click();
        cy.get(".notification-display-v1")
            .contains("Dataset onboarded successfully")
            .should("be.visible");

        cy.url().should("contain", "/configuration/datasets/view/id/");
    });
});
