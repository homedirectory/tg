import '/resources/polymer/@polymer/polymer/polymer-legacy.js';

import { Polymer } from '/resources/polymer/@polymer/polymer/lib/legacy/polymer-fn.js';
import { html } from '/resources/polymer/@polymer/polymer/lib/utils/html-tag.js';

import { getFirstEntityTypeAndProperty } from '/resources/reflection/tg-polymer-utils.js';
import { TgPropertyColumnBehavior } from '/resources/egi/tg-property-column-behavior.js';

const template = html`
    <slot id="action_selector" name="property-action" hidden></slot>
    <slot id="summary_selection" name="summary-property" hidden></slot>`;

Polymer({

    _template: template,

    is: "tg-property-column",

    properties: {
        collectionalProperty: String,
        keyProperty: String,
        valueProperty: String,
        customAction: Object,
        sortable: {
            type: Boolean,
            value: false
        },
        editable: {
            type: Boolean,
            value: false
        }
    },

    behaviors: [TgPropertyColumnBehavior],

    ready: function () {
        const tempSummary = this.$.summary_selection.assignedNodes();
        if (tempSummary.length > 0) {
            this.summary = tempSummary;
        }
        this.customAction = this.$.action_selector.assignedNodes().length > 0 ? this.$.action_selector.assignedNodes()[0] : null;
    },

    /** 
     * Executes a custom action and returns true if the action was provided. 
     * Otherwise, simply returns false to indicate that there was no custom action to be executed. 
     * the passed in currentEntity is a function that returns choosen entity. 
     */
    runAction: function (currentEntity) {
        if (this.customAction) {
            this.customAction.currentEntity = currentEntity;
            this.customAction._run();
            return true;
        }
        return false;
    },

    runDefaultAction: function (currentEntity, defaultPropertyAction) {
        if (defaultPropertyAction) {
            defaultPropertyAction._runDynamicAction(currentEntity, getFirstEntityTypeAndProperty(currentEntity.bind(defaultPropertyAction)(), this.collectionalProperty || this.property)[1]);
            return true;
        } 
        return false;
    }
});